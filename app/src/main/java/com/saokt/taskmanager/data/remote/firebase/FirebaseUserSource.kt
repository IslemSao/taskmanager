package com.saokt.taskmanager.data.remote.firebase

import android.util.Log
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class FirebaseUserSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth // Inject FirebaseAuth to get current user ID safely
) {
    private val invitationsCollection = firestore.collection("project_invitations")
    // --- NEW Listener Flows ---
// Create or update a user document with FCM token
    suspend fun updateUserFcmToken(userId: String, token: String) {
        try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // If the document doesn't exist yet, create it
            val userData = hashMapOf(
                "userId" to userId,
                "fcmToken" to token
            )
            firestore.collection("users").document(userId)
                .set(userData)
                .await()
        }
    }

    fun listenToUserProjects(): Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            trySend(Result.failure(IllegalStateException("User not authenticated")))
            close(CancellationException("User not authenticated"))
            return@callbackFlow
        }

        Log.d("FirestoreListener", "Setting up listeners for projects for user: $userId")
        var ownerListener: ListenerRegistration? = null
        var memberListener: ListenerRegistration? = null
        val memberCollectionListeners = mutableMapOf<String, ListenerRegistration>()

        // Keep track of the latest data
        var latestOwnerProjectDocs = emptyList<DocumentSnapshot>()
        var latestMemberProjectDocs = emptyList<DocumentSnapshot>()
        var latestOwnerProjects = mutableListOf<ProjectDto>()
        var latestMemberProjects = mutableListOf<ProjectDto>()
        var ownerDataReceived = false
        var memberDataReceived = false
        val allMembers = mutableListOf<ProjectMemberDto>()

        fun combineAndSend() {
            if (!ownerDataReceived || !memberDataReceived) return

            val combinedProjects = (latestOwnerProjects + latestMemberProjects).distinctBy { it.id }
            trySend(Result.success(Pair(combinedProjects, allMembers.toList())))
        }

        fun attachMembersListener(projectId: String) {
            if (memberCollectionListeners.containsKey(projectId)) return

            val membersListener = firestore.collection("projects")
                .document(projectId)
                .collection("members")
                .addSnapshotListener { membersSnapshot, membersError ->
                    if (membersError != null) {
                        Log.e("FirestoreListener", "Error listening to members for project $projectId", membersError)
                        return@addSnapshotListener
                    }

                    val members = membersSnapshot?.documents?.mapNotNull { memberDoc ->
                        try {
                            ProjectMemberDto(
                                projectId = projectId,
                                userId = memberDoc.getString("userId") ?: "",
                                email = memberDoc.getString("email") ?: "",
                                displayName = memberDoc.getString("displayName") ?: "",
                                role = memberDoc.getString("role") ?: "MEMBER"
                            )
                        } catch (e: Exception) {
                            Log.e("FirestoreListener", "Error mapping member ${memberDoc.id}", e)
                            null
                        }
                    }.orEmpty()

                    allMembers.removeAll { it.projectId == projectId }
                    allMembers.addAll(members)
                    combineAndSend()
                }

            memberCollectionListeners[projectId] = membersListener
        }

        fun syncMemberListeners() {
            val activeProjectIds = (latestOwnerProjectDocs + latestMemberProjectDocs)
                .map { it.id }
                .toSet()

            memberCollectionListeners.keys
                .filterNot(activeProjectIds::contains)
                .forEach { staleProjectId ->
                    memberCollectionListeners.remove(staleProjectId)?.remove()
                    allMembers.removeAll { it.projectId == staleProjectId }
                }

            activeProjectIds.forEach(::attachMembersListener)
        }

        // Listener for projects where user is owner
        val ownerQuery = firestore.collection("projects").whereEqualTo("ownerId", userId)
        ownerListener = ownerQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreListener", "Owner projects listener error", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                Log.d("FirestoreListener", "Owner projects snapshot received: ${snapshot.documents.size} docs")
                latestOwnerProjectDocs = snapshot.documents
                latestOwnerProjects = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ProjectDto::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreListener", "Map owner err ${doc.id}", e)
                        null
                    }
                }.toMutableList()
                ownerDataReceived = true
                syncMemberListeners()
                combineAndSend()
            }
        }

        // Listener for projects where user is member (unchanged)
        val memberQuery = firestore.collection("projects").whereArrayContains("members", userId)
        memberListener = memberQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreListener", "Member projects listener error", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                Log.d("FirestoreListener", "Member projects snapshot received: ${snapshot.documents.size} docs")
                latestMemberProjectDocs = snapshot.documents
                latestMemberProjects = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ProjectDto::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreListener", "Map member err ${doc.id}", e)
                        null
                    }
                }.toMutableList()
                memberDataReceived = true
                syncMemberListeners()
                combineAndSend()
            }
        }

        awaitClose {
            Log.d("FirestoreListener", "Closing project listeners for user: $userId")
            ownerListener?.remove()
            memberListener?.remove()
            memberCollectionListeners.values.forEach { it.remove() }
        }
    }


    fun listenToUserTasks(): Flow<Result<List<TaskDto>>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            trySend(Result.failure(IllegalStateException("User not authenticated")))
            close(CancellationException("User not authenticated"))
            return@callbackFlow
        }

        Log.d("FirestoreListener", "Setting up listener for tasks for user: $userId")
        val registrations = mutableListOf<ListenerRegistration>()
        val latestTasksByQuery = linkedMapOf<String, List<TaskDto>>()

        fun emitCombinedTasks() {
            val combined = latestTasksByQuery.values
                .flatten()
                .distinctBy { it.id }
                .sortedByDescending { it.modifiedAt }
            Log.d("FirestoreListener", "Sending tasks update: ${combined.size} tasks")
            trySend(Result.success(combined))
        }

        fun attachTaskListener(key: String, field: String, useArrayContains: Boolean = false) {
            val query = if (useArrayContains) {
                firestore.collection("tasks").whereArrayContains(field, userId)
            } else {
                firestore.collection("tasks").whereEqualTo(field, userId)
            }
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Tasks listener error for $field", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(TaskDto::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreListener", "Failed to map task document ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                latestTasksByQuery[key] = tasks
                emitCombinedTasks()
            }
            registrations += registration
        }

        attachTaskListener("visible", "visibleToUserIds", useArrayContains = true)
        attachTaskListener("creator", "createdBy")
        attachTaskListener("assignee", "assignedTo")
        attachTaskListener("owner", "userId")

        awaitClose {
            Log.d("FirestoreListener", "Closing task listener for user: $userId")
            registrations.forEach { it.remove() }
        }
    }

    fun listenToInvitations(): Flow<Result<List<ProjectInvitationDto>>> = callbackFlow {
        // Auth checks remain the same
        val userId = firebaseAuth.currentUser?.uid
        val email = firebaseAuth.currentUser?.email
        if (userId == null) {
            Log.w("FirestoreListener", "User not authenticated for invitations listener.")
            trySend(Result.failure(IllegalStateException("User not authenticated")))
            close(CancellationException("User not authenticated"))
            return@callbackFlow
        }

        Log.d("FirestoreListener", "Setting up listener for invitations for user: $userId")
        var invitationListener: ListenerRegistration? = null

        try {
            val query = invitationsCollection.whereEqualTo("inviteeEmail", email)

            invitationListener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreListener", "Invitations listener error for user $userId", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("FirestoreListener", "Invitations snapshot received: ${snapshot.documents.size} docs for user $userId")
                    val invitations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toObject(ProjectInvitationDto::class.java)
                            dto?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("FirestoreListener", "Error mapping document ${doc.id} to ProjectInvitationDto", e)
                            null
                        }
                    }

                    Log.d("FirestoreListener", "Sending invitations update: ${invitations.size} invitations for user $userId")
                    trySend(Result.success(invitations))
                } else {
                    Log.w("FirestoreListener", "Invitations snapshot was null, but no error reported for user $userId")
                    trySend(Result.success(emptyList()))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreListener", "Error setting up invitations listener for user $userId", e)
            trySend(Result.failure(e))
            close(e)
            return@callbackFlow
        }

        awaitClose {
            Log.d("FirestoreListener", "Closing invitation listener for user: $userId")
            invitationListener?.remove()
        }
    }

}
