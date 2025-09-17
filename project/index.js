/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { setGlobalOptions } = require("firebase-functions");
const { onDocumentCreated, onDocumentWritten, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();
setGlobalOptions({ maxInstances: 10 });

// 1. Send notification on new project invitation
exports.sendProjectInvitationNotification = onDocumentCreated({
  document: 'project_invitations/{invitationId}'
}, async (event) => {
  const snap = event.data;
  if (!snap) {
    console.log('[InvitationNotif] No snapshot data, exiting');
    return null;
  }
  const invitation = snap.data();
  const inviteeEmail = invitation.inviteeEmail;
  const inviterName = invitation.inviterName || 'Someone';
  const projectTitle = invitation.projectTitle || 'a project';
  console.log(`[InvitationNotif] New invitation for email: ${inviteeEmail}, inviter: ${inviterName}, project: ${projectTitle}`);

  // Find user by email
  const userQuery = await db.collection('users').where('email', '==', inviteeEmail).get();
  if (userQuery.empty) {
    console.log(`[InvitationNotif] No user found with email: ${inviteeEmail}`);
    return null;
  }
  const userDoc = userQuery.docs[0];
  const fcmToken = userDoc.get('fcmToken');
  if (!fcmToken) {
    console.log(`[InvitationNotif] No fcmToken for user: ${inviteeEmail}`);
    return null;
  }

  const payload = {
    notification: {
      title: 'Project Invitation',
      body: `${inviterName} invited you to join "${projectTitle}"`,
    },
    data: {
      type: 'project_invitation',
      projectId: invitation.projectId,
      invitationId: snap.id,
    }
  };

  try {
    const response = await admin.messaging().send({
      token: fcmToken,
      notification: payload.notification,
      data: payload.data
    });
    console.log(`[InvitationNotif] Notification sent to ${inviteeEmail}, response:`, response);
  } catch (err) {
    console.error(`[InvitationNotif] Error sending notification to ${inviteeEmail}:`, err);
  }
  return null;
});

// 2. Send notification on task assignment (only if assigned by someone else)
exports.sendTaskAssignmentNotification = onDocumentWritten({
  document: 'tasks/{taskId}'
}, async (event) => {
  const before = event.data?.before?.data() || null;
  const after = event.data?.after?.data() || null;

  // Only notify if assignedTo changed and is not the creator
  if (!after || !after.assignedTo || after.assignedTo === after.createdBy) return null;
  if (before && before.assignedTo === after.assignedTo) return null; // not a new assignment

  // Find user by userId
  const userDoc = await db.collection('users').doc(after.assignedTo).get();
  if (!userDoc.exists) return null;
  const fcmToken = userDoc.get('fcmToken');
  if (!fcmToken) return null;

  // Get assigner's name
  let assignerName = 'Someone';
  if (after.assignedBy) {
    const assignerDoc = await db.collection('users').doc(after.assignedBy).get();
    if (assignerDoc.exists) {
      assignerName = assignerDoc.get('displayName') || assignerDoc.get('email') || 'Someone';
    }
  }

  const payload = {
    notification: {
      title: `Task assigned by ${assignerName}`,
      body: `You have been assigned: "${after.title}"`,
    },
    data: {
      type: 'task_assignment',
      taskId: event.params.taskId,
      projectId: after.projectId || '',
    }
  };

  await admin.messaging().send({
    token: fcmToken,
    notification: payload.notification,
    data: payload.data
  });
  return null;
});

// 4. Notify on task completion (to project members)
exports.sendTaskCompletionNotification = onDocumentUpdated({
  document: 'tasks/{taskId}'
}, async (event) => {
  const before = event.data?.before?.data() || null;
  const after = event.data?.after?.data() || null;

  // Only notify if task was marked as completed
  if (!after || !before || before.completed || !after.completed) return null;
  if (!after.projectId) return null; // Only for project tasks

  // Get project members
  const projectDoc = await db.collection('projects').doc(after.projectId).get();
  if (!projectDoc.exists) return null;
  const project = projectDoc.data();
  const memberIds = project.memberIds || [];
  
  // Don't notify the person who completed the task
  const notifyIds = memberIds.filter(id => id !== after.userId);
  if (notifyIds.length === 0) return null;

  // Get completer's name
  let completerName = 'Someone';
  if (after.userId) {
    const userDoc = await db.collection('users').doc(after.userId).get();
    if (userDoc.exists) {
      completerName = userDoc.get('displayName') || userDoc.get('email') || 'Someone';
    }
  }

  // Fetch FCM tokens
  const tokens = [];
  for (const uid of notifyIds) {
    const udoc = await db.collection('users').doc(uid).get();
    if (udoc.exists) {
      const t = udoc.get('fcmToken');
      if (t) tokens.push(t);
    }
  }

  if (tokens.length === 0) return null;

  const payload = {
    notification: {
      title: `Task completed in ${project.name}`,
      body: `${completerName} completed: "${after.title}"`,
    },
    data: {
      type: 'task_completion',
      taskId: event.params.taskId,
      projectId: after.projectId,
    }
  };

  try {
    const response = await admin.messaging().sendEachForMulticast({
      tokens,
      notification: payload.notification,
      data: payload.data
    });
    console.log('[TaskCompletion] Sent to', tokens.length, 'recipients', response.successCount);
  } catch (err) {
    console.error('[TaskCompletion] Error sending notifications', err);
  }
  return null;
});

exports.notifyOwnerOnInvitationResponse = onDocumentWritten({
  document: 'project_invitations/{invitationId}'
}, async (event) => {
  const before = event.data?.before?.data() || {};
  const after = event.data?.after?.data() || {};

  // Only notify if status changed and is now ACCEPTED or REJECTED
  if (before.status === after.status) return null;
  if (!['ACCEPTED', 'REJECTED'].includes(after.status)) return null;

  const inviterId = after.inviterId;
  const inviteeName = after.inviteeName || after.inviteeEmail || 'A user';
  const projectTitle = after.projectTitle || 'your project';

  // Find inviter's FCM token
  const ownerDoc = await db.collection('users').doc(inviterId).get();
  if (!ownerDoc.exists) {
    console.log(`[OwnerNotif] No user found with id: ${inviterId}`);
    return null;
  }
  const fcmToken = ownerDoc.get('fcmToken');
  if (!fcmToken) {
    console.log(`[OwnerNotif] No fcmToken for user: ${inviterId}`);
    return null;
  }

  const payload = {
    notification: {
      title: `Invitation ${after.status.toLowerCase()}`,
      body: `${inviteeName} has ${after.status.toLowerCase()} your invitation to join "${projectTitle}".`,
    },
    data: {
      type: 'invitation_response',
      projectId: after.projectId,
      invitationId: event.params.invitationId,
      status: after.status
    }
  };

  try {
    const response = await admin.messaging().send({
      token: fcmToken,
      notification: payload.notification,
      data: payload.data
    });
    console.log(`[OwnerNotif] Notification sent to owner ${inviterId}, response:`, response);
  } catch (err) {
    console.error(`[OwnerNotif] Error sending notification to owner ${inviterId}:`, err);
  }
  return null;
});

// 3. Send notification to participants on new chat message
exports.sendChatMessageNotification = onDocumentCreated({
  document: 'chat_threads/{threadId}/messages/{messageId}'
}, async (event) => {
  const snap = event.data;
  if (!snap) return null;
  const msg = snap.data();
  const threadId = event.params.threadId;

  // Load thread to get participants
  const threadDoc = await db.collection('chat_threads').doc(threadId).get();
  if (!threadDoc.exists) return null;
  const thread = threadDoc.data();
  const participants = thread.participantIds || [];
  const senderId = msg.senderId;

  const notifyIds = participants.filter((id) => id !== senderId);
  if (notifyIds.length === 0) return null;

  // Fetch FCM tokens
  const tokens = [];
  for (const uid of notifyIds) {
    const udoc = await db.collection('users').doc(uid).get();
    if (udoc.exists) {
      const t = udoc.get('fcmToken');
      if (t) tokens.push(t);
    }
  }

  if (tokens.length === 0) return null;

  const payload = {
    notification: {
      title: `New message from ${msg.senderName || msg.senderId || 'Someone'}`,
      body: msg.text?.slice(0, 120) || 'You have a new chat message',
    },
    data: {
      type: 'chat_message',
      threadId: threadId,
      projectId: thread.projectId || '',
      taskId: thread.taskId || ''
    }
  };

  try {
    const response = await admin.messaging().sendEachForMulticast({
      tokens,
      notification: payload.notification,
      data: payload.data
    });
    console.log('[ChatNotif] Sent to', tokens.length, 'recipients', response.successCount);
  } catch (err) {
    console.error('[ChatNotif] Error sending notifications', err);
  }
  return null;
});
