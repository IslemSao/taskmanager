package com.example.taskmanager.data.remote.firebase

import android.util.Log
import com.example.taskmanager.data.remote.dto.UserDto
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
    private suspend fun saveFcmToken(email: String) {
        try {
            // Get FCM token
            val fcmToken = FirebaseMessaging.getInstance().token.await()

            // Store in Firestore (users collection)
            firestore.collection("users")
                .document(email)
                .set(mapOf("fcmToken" to fcmToken), SetOptions.merge())
                .await()

            Log.d("FCM", "Token saved for user $email")
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
}
