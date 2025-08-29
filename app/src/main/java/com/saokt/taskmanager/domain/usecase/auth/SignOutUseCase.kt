package com.saokt.taskmanager.domain.usecase.auth

import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.notification.FCMTokenManager
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val fcmTokenManager: FCMTokenManager
) {
    suspend operator fun invoke(): Result<Unit> {
        // First remove FCM token from Firestore
        val tokenRemovalResult = fcmTokenManager.removeTokenFromFirestore()

        // Then sign out from the repository
        val signOutResult = userRepository.signOut()

        // Return success only if both operations succeed
        return if (tokenRemovalResult.isSuccess && signOutResult.isSuccess) {
            Result.success(Unit)
        } else {
            // If token removal fails but sign out succeeds, still consider it successful
            // since the user is signed out, but log the token removal failure
            if (tokenRemovalResult.isFailure) {
                // You could log this or handle it as needed
            }
            signOutResult
        }
    }
} 