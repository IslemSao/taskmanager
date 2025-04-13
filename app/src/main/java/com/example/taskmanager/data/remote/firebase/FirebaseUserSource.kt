package com.example.taskmanager.data.remote.firebase

import android.util.Log
import com.example.taskmanager.data.remote.dto.ProjectDto
import com.example.taskmanager.data.remote.dto.ProjectInvitationDto
import com.example.taskmanager.data.remote.dto.ProjectMemberDto
import com.example.taskmanager.data.remote.dto.TaskDto
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
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
        var latestOwnerProjects = mutableListOf<ProjectDto>()
        var latestMemberProjects = mutableListOf<ProjectDto>()
        var ownerDataReceived = false
        var memberDataReceived = false
        val allMembers = mutableListOf<ProjectMemberDto>()

        fun combineAndSend() {
            if (!ownerDataReceived || !memberDataReceived) return

            val combinedProjects = (latestOwnerProjects + latestMemberProjects).distinctBy { it.id }
            Log.d("FirestoreListener", "Sending combined projects update: ${combinedProjects.size} projects")
            Log.d("FirestoreListener", "Current members count: ${allMembers.size}")
            allMembers.forEach { member ->
                Log.d("FirestoreListener", "Member: ${member.userId} in project ${member.projectId}, role: ${member.role}")
            }
            trySend(Result.success(Pair(combinedProjects, allMembers.toList())))
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

                // Clear listeners for projects we're no longer owner of
                val currentOwnerProjectIds = snapshot.documents.map { it.id }.toSet()
                memberCollectionListeners.keys.filterNot { currentOwnerProjectIds.contains(it) }
                    .forEach { projectId ->
                        Log.d("FirestoreListener", "Removing listener for old project: $projectId")
                        memberCollectionListeners[projectId]?.remove()
                        memberCollectionListeners.remove(projectId)
                        allMembers.removeAll { it.projectId == projectId }
                    }

                latestOwnerProjects = snapshot.documents.mapNotNull { doc ->
                    try {
                        val project = doc.toObject(ProjectDto::class.java)?.copy(id = doc.id)

                        // Set up listener for members subcollection
                        if (project != null && !memberCollectionListeners.containsKey(doc.id)) {
                            Log.d("FirestoreListener", "Setting up members listener for project: ${doc.id}")
                            val membersListener = firestore.collection("projects")
                                .document(doc.id)
                                .collection("members")
                                .addSnapshotListener { membersSnapshot, membersError ->
                                    if (membersError != null) {
                                        Log.e("FirestoreListener", "Error listening to members for project ${doc.id}", membersError)
                                        return@addSnapshotListener
                                    }

                                    membersSnapshot?.let { ms ->
                                        Log.d("FirestoreListener", "Members snapshot received for project ${doc.id}:")
                                        Log.d("FirestoreListener", "Document count: ${ms.documentChanges.size}")
                                        ms.documentChanges.forEach { change ->
                                            Log.d("FirestoreListener", "Change type: ${change.type}, Member ID: ${change.document.id}")
                                            Log.d("FirestoreListener", "Member data: ${change.document.data}")
                                        }

                                        // Remove old members for this project
                                        val removedCount = allMembers.removeAll { it.projectId == doc.id }
                                        Log.d("FirestoreListener", "Removed $removedCount old members for project ${doc.id}")

                                        // Add new members with manual mapping
                                        val members = ms.documents.mapNotNull { memberDoc ->
                                            try {
                                                Log.d("FirestoreListener", "Processing member document: ${memberDoc.id}")
                                                Log.d("FirestoreListener", "Member data: ${memberDoc.data}")

                                                ProjectMemberDto(
                                                    projectId = doc.id,
                                                    userId = memberDoc.getString("userId") ?: "",
                                                    email = memberDoc.getString("email") ?: "",
                                                    displayName = memberDoc.getString("displayName") ?: "",
                                                    role = memberDoc.getString("role") ?: "MEMBER"
                                                ).also {
                                                    Log.d("FirestoreListener", "Mapped member: $it")
                                                }
                                            } catch (e: Exception) {
                                                Log.e("FirestoreListener", "Error mapping member ${memberDoc.id}", e)
                                                null
                                            }
                                        }
                                        Log.d("FirestoreListener", "Adding ${members.size} members for project ${doc.id}")
                                        allMembers.addAll(members)
                                        combineAndSend()
                                    }
                                }
                            memberCollectionListeners[doc.id] = membersListener
                        }

                        project
                    } catch (e: Exception) {
                        Log.e("FirestoreListener", "Map owner err ${doc.id}", e)
                        null
                    }
                }.toMutableList()
                ownerDataReceived = true
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
                latestMemberProjects = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ProjectDto::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreListener", "Map member err ${doc.id}", e)
                        null
                    }
                }.toMutableList()
                memberDataReceived = true
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
        var taskListener: ListenerRegistration? = null

        val query = firestore.collection("tasks").whereEqualTo("userId", userId)
        taskListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreListener", "Tasks listener error", error)
                trySend(Result.failure(error))
                // close(error) // Close flow on error?
                return@addSnapshotListener
            }
            if (snapshot != null) {
                Log.d(
                    "FirestoreListener",
                    "Tasks snapshot received: ${snapshot.documents.size} docs"
                )
                val tasks = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(TaskDto::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreListener", "Failed to map task document ${doc.id}", e)
                        null
                    }
                }
                Log.d("FirestoreListener", "Sending tasks update: ${tasks.size} tasks")
                trySend(Result.success(tasks))
            }
        }

        awaitClose {
            Log.d("FirestoreListener", "Closing task listener for user: $userId")
            taskListener?.remove()
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

                    // Fixed: Properly map documents to DTOs once
                    val invitations = snapshot.documents.mapNotNull { doc ->
                        try {
                            Log.d("bombardiro", "doc: $doc")
                            val dto = doc.toObject(ProjectInvitationDto::class.java)
                            Log.d("bombardiro", "dto: $dto")
                            dto?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("FirestoreListener", "Error mapping document ${doc.id} to ProjectInvitationDto", e)
                            null
                        }
                    }

                    // Important: Actually send the mapped results to the Flow
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