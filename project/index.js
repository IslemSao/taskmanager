/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { setGlobalOptions } = require("firebase-functions");
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
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

  const payload = {
    notification: {
      title: 'New Task Assigned',
      body: `You have been assigned a new task: "${after.title}"`,
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
