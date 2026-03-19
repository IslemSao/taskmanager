package com.saokt.taskmanager.data.remote.firebase

import android.util.Log
import com.saokt.taskmanager.data.remote.dto.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore // Add Firestore dependency
) {
    // Helper function to get/save FCM token
    private suspend fun saveFcmToken(userId: String) {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()

            firestore.collection("users")
                .document(userId)
                .set(mapOf("fcmToken" to fcmToken), SetOptions.merge())
                .await()

            Log.d("FCM", "Token saved for user $userId")
        } catch (e: Exception) {
            Log.e("FCM", "Failed to save token", e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserDto> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication failed"))

            saveFcmToken(user.uid)
            Result.success(
                UserDto(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun signInWithGoogle(idToken: String): Result<UserDto> {
        val TAG = "GoogleSignIn"

        return try {
            Log.d(TAG, "Starting Google sign-in with ID token.")

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d(TAG, "Credential created.")

            val authResult = auth.signInWithCredential(credential).await()
            Log.d(TAG, "Authentication result received.")

            val user = authResult.user
            if (user == null) {
                Log.e(TAG, "Authentication failed: user is null.")
                return Result.failure(IllegalStateException("Authentication failed"))
            }

            saveFcmToken(user.uid)
            val userDto = UserDto(
                id = user.uid,
                email = user.email ?: "",
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString()
            )

            Log.d(TAG, "User signed in successfully: $userDto")
            Result.success(userDto)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<UserDto> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("User creation failed"))

            saveFcmToken(user.uid)
            // Update display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            user.updateProfile(profileUpdates).await()

            Result.success(
                UserDto(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = displayName,
                    photoUrl = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): UserDto? {
        val user = auth.currentUser ?: return null
        return UserDto(
            id = user.uid,
            email = user.email ?: "",
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("No user signed in"))

            val userId = currentUser.uid

            firestore.collection("users").document(userId).delete().await()

            val userProjects = firestore.collection("projects")
                .whereEqualTo("ownerId", userId)
                .get()
                .await()

            for (project in userProjects.documents) {
                val projectTasks = firestore.collection("tasks")
                    .whereEqualTo("projectId", project.id)
                    .get()
                    .await()
                val projectInvitations = firestore.collection("project_invitations")
                    .whereEqualTo("projectId", project.id)
                    .get()
                    .await()
                val projectMembers = firestore.collection("projects")
                    .document(project.id)
                    .collection("members")
                    .get()
                    .await()

                val batch = firestore.batch()
                projectTasks.documents.forEach { batch.delete(it.reference) }
                projectInvitations.documents.forEach { batch.delete(it.reference) }
                projectMembers.documents.forEach { batch.delete(it.reference) }
                batch.delete(project.reference)
                batch.commit().await()
            }

            val memberProjects = firestore.collection("projects")
                .whereArrayContains("members", userId)
                .get()
                .await()

            for (project in memberProjects.documents) {
                if (project.getString("ownerId") == userId) {
                    continue
                }
                val batch = firestore.batch()
                batch.delete(
                    firestore.collection("projects")
                        .document(project.id)
                        .collection("members")
                        .document(userId)
                )
                batch.update(
                    project.reference,
                    mapOf(
                        "members" to com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "memberIds" to com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                    )
                )
                batch.commit().await()
            }

            val assignedTasks = firestore.collection("tasks")
                .whereEqualTo("assignedTo", userId)
                .get()
                .await()
            assignedTasks.documents.forEach { task ->
                val visibleTo = (task.get("visibleToUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                task.reference.update(
                    mapOf(
                        "assignedTo" to null,
                        "assignedAt" to null,
                        "assignedBy" to null,
                        "visibleToUserIds" to visibleTo.filterNot { it == userId }
                    )
                ).await()
            }

            val userInvitations = firestore.collection("project_invitations")
                .whereEqualTo("inviteeEmail", currentUser.email)
                .get()
                .await()
            userInvitations.documents.forEach { it.reference.delete().await() }

            val sentInvitations = firestore.collection("project_invitations")
                .whereEqualTo("inviterId", userId)
                .get()
                .await()
            sentInvitations.documents.forEach { it.reference.delete().await() }

            val userCreatedTasks = firestore.collection("tasks")
                .whereEqualTo("createdBy", userId)
                .get()
                .await()
            userCreatedTasks.documents.forEach { it.reference.delete().await() }

            val ownedTasks = firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            ownedTasks.documents.forEach { it.reference.delete().await() }

            currentUser.delete().await()

            Log.d("FirebaseAuthSource", "Account and all associated data deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthSource", "Failed to delete account", e)
            Result.failure(e)
        }
    }
}
