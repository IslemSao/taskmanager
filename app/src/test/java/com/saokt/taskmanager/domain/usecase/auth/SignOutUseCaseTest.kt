package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.notification.FCMTokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignOutUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val fcmTokenManager = mockk<FCMTokenManager>()
    private val signOutUseCase = SignOutUseCase(userRepository, fcmTokenManager)

    @Test
    fun `invoke succeeds when token removal and sign out both succeed`() = runTest {
        coEvery { fcmTokenManager.removeTokenFromFirestore() } returns Result.success(Unit)
        coEvery { userRepository.signOut() } returns Result.success(Unit)

        val result = signOutUseCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { fcmTokenManager.removeTokenFromFirestore() }
        coVerify(exactly = 1) { userRepository.signOut() }
    }

    @Test
    fun `invoke still succeeds when token removal fails but sign out succeeds`() = runTest {
        coEvery {
            fcmTokenManager.removeTokenFromFirestore()
        } returns Result.failure(IllegalStateException("Token cleanup failed"))
        coEvery { userRepository.signOut() } returns Result.success(Unit)

        val result = signOutUseCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { fcmTokenManager.removeTokenFromFirestore() }
        coVerify(exactly = 1) { userRepository.signOut() }
    }

    @Test
    fun `invoke returns sign out failure when repository sign out fails`() = runTest {
        val error = IllegalStateException("Sign out failed")
        coEvery { fcmTokenManager.removeTokenFromFirestore() } returns Result.success(Unit)
        coEvery { userRepository.signOut() } returns Result.failure(error)

        val result = signOutUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 1) { fcmTokenManager.removeTokenFromFirestore() }
        coVerify(exactly = 1) { userRepository.signOut() }
    }

    @Test
    fun `invoke still attempts sign out when token removal fails`() = runTest {
        val error = IllegalStateException("Sign out failed")
        coEvery {
            fcmTokenManager.removeTokenFromFirestore()
        } returns Result.failure(IllegalStateException("Token cleanup failed"))
        coEvery { userRepository.signOut() } returns Result.failure(error)

        val result = signOutUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 1) { fcmTokenManager.removeTokenFromFirestore() }
        coVerify(exactly = 1) { userRepository.signOut() }
    }
}
