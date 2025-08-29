package com.saokt.taskmanager.notification

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "FCMTokenManager"
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_LAST_TOKEN = "last_fcm_token"
        private const val KEY_LAST_UPDATE = "last_token_update"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Refresh and update FCM token to Firestore
     * Call this method when the app starts or when user logs in
     */
    suspend fun refreshAndUpdateToken(): Result<String> {
        return try {
            // Get current FCM token
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "Retrieved FCM token: ${token.take(20)}...")

            // Check if user is authenticated
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not authenticated, skipping token update")
                return Result.failure(Exception("User not authenticated"))
            }

            // Check if token has changed since last update
            val lastToken = getLastStoredToken()
            val lastUpdate = getLastUpdateTime()
            val currentTime = System.currentTimeMillis()

            // Update token if:
            // 1. It's a new token, or
            // 2. It's been more than 24 hours since last update (safety check)
            if (token != lastToken || (currentTime - lastUpdate) > (24 * 60 * 60 * 1000)) {
                updateTokenInFirestore(userId, token)
                storeTokenLocally(token, currentTime)
                Log.d(TAG, "FCM token updated successfully for user: $userId")
                Result.success(token)
            } else {
                Log.d(TAG, "FCM token is up to date, skipping update")
                Result.success(token)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Update FCM token in Firestore
     */
    private suspend fun updateTokenInFirestore(userId: String, token: String) {
        try {
            val userRef = firestore.collection("users").document(userId)
            val updateData = hashMapOf<String, Any>(
                "fcmToken" to token,
                "tokenLastUpdated" to System.currentTimeMillis(),
                "deviceInfo" to mapOf(
                    "platform" to "android",
                    "appVersion" to getAppVersion(),
                    "lastActive" to System.currentTimeMillis()
                )
            )

            userRef.update(updateData).await()
            Log.d(TAG, "FCM token updated in Firestore for user: $userId")

        } catch (e: Exception) {
            // If update fails, try to set the token (in case the document doesn't exist)
            try {
                val userRef = firestore.collection("users").document(userId)
                val setData = hashMapOf<String, Any>(
                    "fcmToken" to token,
                    "tokenLastUpdated" to System.currentTimeMillis(),
                    "deviceInfo" to mapOf(
                        "platform" to "android",
                        "appVersion" to getAppVersion(),
                        "lastActive" to System.currentTimeMillis()
                    )
                )
                userRef.set(setData).await()
                Log.d(TAG, "FCM token set in Firestore for user: $userId")
            } catch (setException: Exception) {
                Log.e(TAG, "Failed to set FCM token in Firestore", setException)
                throw setException
            }
        }
    }

    /**
     * Remove FCM token from Firestore (call on logout)
     */
    suspend fun removeTokenFromFirestore(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not authenticated, skipping token removal")
                return Result.failure(Exception("User not authenticated"))
            }

            val userRef = firestore.collection("users").document(userId)
            val updateData = hashMapOf<String, Any>(
                "fcmToken" to "",
                "tokenLastUpdated" to System.currentTimeMillis()
            )

            userRef.update(updateData).await()
            clearStoredToken()
            Log.d(TAG, "FCM token removed from Firestore for user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove FCM token from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Get the current FCM token without updating Firestore
     */
    suspend fun getCurrentToken(): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Check if FCM token needs refreshing
     */
    fun shouldRefreshToken(): Boolean {
        val lastUpdate = getLastUpdateTime()
        val currentTime = System.currentTimeMillis()
        // Refresh if it's been more than 24 hours or never updated
        return (currentTime - lastUpdate) > (24 * 60 * 60 * 1000) || lastUpdate == 0L
    }

    // ========== PRIVATE HELPER METHODS ==========

    private fun getLastStoredToken(): String? {
        return sharedPreferences.getString(KEY_LAST_TOKEN, null)
    }

    private fun getLastUpdateTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_UPDATE, 0L)
    }

    private fun storeTokenLocally(token: String, updateTime: Long) {
        sharedPreferences.edit()
            .putString(KEY_LAST_TOKEN, token)
            .putLong(KEY_LAST_UPDATE, updateTime)
            .apply()
    }

    private fun clearStoredToken() {
        sharedPreferences.edit()
            .remove(KEY_LAST_TOKEN)
            .remove(KEY_LAST_UPDATE)
            .apply()
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
