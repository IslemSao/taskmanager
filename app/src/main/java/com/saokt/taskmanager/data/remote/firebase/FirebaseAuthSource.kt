package com.saokt.taskmanager.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.UserDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseAuthSource"
        private const val USERS_COLLECTION = "users"
        private const val PROJECTS_COLLECTION = "projects"
        private const val TASKS_COLLECTION = "tasks"
        private const val INVITATIONS_COLLECTION = "project_invitations"
        private const val MEMBERS_SUBCOLLECTION = "members"

        internal fun mapAuthException(exception: Exception): Exception {
            return when (exception) {
                is FirebaseNetworkException -> IllegalStateException(
                    "Check your internet connection and try again.",
                    exception
                )
                is FirebaseTooManyRequestsException -> IllegalStateException(
                    "Too many attempts. Please wait and try again.",
                    exception
                )
                is FirebaseAuthRecentLoginRequiredException -> IllegalStateException(
                    "For security, please sign in again before deleting your account.",
                    exception
                )
                is FirebaseFirestoreException -> {
                    if (exception.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                        IllegalStateException(
                            "Check your internet connection and try again.",
                            exception
                        )
                    } else {
                        exception
                    }
                }
                else -> exception
            }
        }
    }

    private suspend fun saveFcmToken(userId: String) {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(mapOf("fcmToken" to fcmToken), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save FCM token", e)
        }
    }

    private suspend fun ensureUserDocument(user: FirebaseUser, displayNameOverride: String? = null) {
        val displayName = displayNameOverride ?: user.displayName
        val normalizedEmail = normalizeEmail(user.email)
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "userId" to user.uid,
                    "email" to normalizedEmail,
                    "displayName" to displayName,
                    "photoUrl" to user.photoUrl?.toString()
                )
            )
            .await()
    }

    private suspend fun syncUserMetadataBestEffort(user: FirebaseUser, displayNameOverride: String? = null) {
        try {
            ensureUserDocument(user, displayNameOverride)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync user profile document for ${user.uid}", e)
        }

        saveFcmToken(user.uid)
    }

    suspend fun signIn(email: String, password: String): Result<UserDto> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication failed"))

            syncUserMetadataBestEffort(user)
            Result.success(
                UserDto(
                    id = user.uid,
                    email = normalizeEmail(user.email),
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString()
                )
            )
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun signInWithGoogle(idToken: String): Result<UserDto> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user == null) {
                return Result.failure(IllegalStateException("Authentication failed"))
            }

            syncUserMetadataBestEffort(user)
            val userDto = UserDto(
                id = user.uid,
                email = normalizeEmail(user.email),
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString()
            )
            Result.success(userDto)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<UserDto> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("User creation failed"))

            saveFcmToken(user.uid)
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            user.updateProfile(profileUpdates).await()
            syncUserMetadataBestEffort(user, displayName)

            Result.success(
                UserDto(
                    id = user.uid,
                    email = normalizeEmail(user.email),
                    displayName = displayName,
                    photoUrl = null
                )
            )
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("No user signed in"))

            currentUser.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun isCurrentUserEmailVerified(forceRefresh: Boolean = false): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("No user signed in"))

            if (forceRefresh) {
                currentUser.reload().await()
            }

            Result.success(currentUser.isEmailVerified)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
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
            email = normalizeEmail(user.email),
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString()
        )
    }

    private fun normalizeEmail(email: String?): String {
        return email?.trim()?.lowercase().orEmpty()
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("No user signed in"))

            val userId = currentUser.uid
            val emailCandidates = buildList {
                currentUser.email?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                currentUser.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { add(it) }
            }.distinct()

            firestore.collection(USERS_COLLECTION).document(userId).delete().await()

            val userProjects = firestore.collection(PROJECTS_COLLECTION)
                .whereEqualTo("ownerId", userId)
                .get()
                .await()

            for (project in userProjects.documents) {
                val projectTasks = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("projectId", project.id)
                    .get()
                    .await()
                val projectInvitations = firestore.collection(INVITATIONS_COLLECTION)
                    .whereEqualTo("projectId", project.id)
                    .get()
                    .await()
                val projectMembers = firestore.collection(PROJECTS_COLLECTION)
                    .document(project.id)
                    .collection(MEMBERS_SUBCOLLECTION)
                    .get()
                    .await()

                val batch = firestore.batch()
                projectTasks.documents.forEach { batch.delete(it.reference) }
                projectInvitations.documents.forEach { batch.delete(it.reference) }
                projectMembers.documents.forEach { batch.delete(it.reference) }
                batch.delete(project.reference)
                batch.commit().await()
            }

            val memberProjectMemberships = getMemberProjectMemberships(userId)

            for ((projectSnapshot, membershipReference) in memberProjectMemberships) {
                val projectDto = projectSnapshot.toObject(ProjectDto::class.java)
                if (projectDto?.ownerId == userId) {
                    continue
                }
                val batch = firestore.batch()
                batch.delete(membershipReference)
                batch.update(
                    projectSnapshot.reference,
                    mapOf(
                        "members" to com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "memberIds" to com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                    )
                )
                batch.commit().await()
            }

            val assignedTasks = firestore.collection(TASKS_COLLECTION)
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

            getInvitationSnapshotsForInviteeEmails(emailCandidates)
                .flatMap { it.documents }
                .distinctBy { it.id }
                .forEach { it.reference.delete().await() }

            val sentInvitations = firestore.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("inviterId", userId)
                .get()
                .await()
            sentInvitations.documents.forEach { it.reference.delete().await() }

            val userCreatedTasks = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("createdBy", userId)
                .get()
                .await()
            userCreatedTasks.documents.forEach { it.reference.delete().await() }

            val ownedTasks = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            ownedTasks.documents.forEach { it.reference.delete().await() }

            currentUser.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete account", e)
            Result.failure(mapAuthException(e))
        }
    }

    private suspend fun getMemberProjectMemberships(userId: String): List<Pair<DocumentSnapshot, com.google.firebase.firestore.DocumentReference>> {
        val failures = mutableListOf<Throwable>()
        val cleanupTargets = mutableListOf<Pair<DocumentSnapshot, com.google.firebase.firestore.DocumentReference>>()
        var successfulLookups = 0

        listOf("members", "memberIds").forEach { field ->
            runCatching {
                firestore.collection(PROJECTS_COLLECTION)
                    .whereArrayContains(field, userId)
                    .get()
                    .await()
            }.onSuccess { querySnapshot ->
                successfulLookups += 1
                cleanupTargets += querySnapshot.documents
                    .filter { it.exists() }
                    .map { projectSnapshot ->
                        projectSnapshot to projectSnapshot.reference
                            .collection(MEMBERS_SUBCOLLECTION)
                            .document(userId)
                    }
            }.onFailure { failures += it }
        }

        runCatching {
            firestore.collectionGroup(MEMBERS_SUBCOLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
        }.onSuccess { querySnapshot ->
            successfulLookups += 1
            cleanupTargets += querySnapshot.documents.mapNotNull { membershipSnapshot ->
                val projectReference = membershipSnapshot.reference.parent.parent ?: return@mapNotNull null
                val projectSnapshot = runCatching { projectReference.get().await() }.getOrNull()
                    ?: return@mapNotNull null
                if (!projectSnapshot.exists()) return@mapNotNull null
                projectSnapshot to membershipSnapshot.reference
            }
        }.onFailure { failures += it }

        if (successfulLookups == 0 && failures.isNotEmpty()) {
            throw IllegalStateException(
                "Failed to resolve project memberships for account cleanup",
                failures.first()
            )
        }

        return cleanupTargets.distinctBy { (projectSnapshot, _) -> projectSnapshot.id }
    }

    private suspend fun getInvitationSnapshotsForInviteeEmails(emails: List<String>) =
        if (emails.isEmpty()) {
            emptyList()
        } else {
            emails.map { email ->
            firestore.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("inviteeEmail", email)
                .get()
                .await()
            }
        }
}
