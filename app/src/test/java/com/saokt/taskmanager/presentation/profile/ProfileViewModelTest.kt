package com.saokt.taskmanager.presentation.profile

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.domain.usecase.auth.CheckEmailVerificationStatusUseCase
import com.saokt.taskmanager.domain.usecase.auth.DeleteAccountUseCase
import com.saokt.taskmanager.domain.usecase.auth.SendEmailVerificationUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignOutUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import com.saokt.taskmanager.notification.FCMTokenManager
import com.saokt.taskmanager.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>()
    private val fcmTokenManager = mockk<FCMTokenManager>(relaxed = true)
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        every { userRepository.getCurrentUser() } returns flowOf(
            User(id = "u1", email = "jane@example.com", displayName = "Jane")
        )
        coEvery { userRepository.deleteAccount() } returns Result.success(Unit)
        coEvery { userRepository.sendEmailVerification() } returns Result.success(Unit)
        coEvery { userRepository.isCurrentUserEmailVerified(false) } returns Result.success(false)
        coEvery { userRepository.isCurrentUserEmailVerified(true) } returns Result.success(false)

        viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            signOutUseCase = SignOutUseCase(userRepository, fcmTokenManager),
            deleteAccountUseCase = DeleteAccountUseCase(userRepository),
            sendEmailVerificationUseCase = SendEmailVerificationUseCase(userRepository),
            checkEmailVerificationStatusUseCase = CheckEmailVerificationStatusUseCase(userRepository)
        )
    }

    @Test
    fun `init loads current user and clears loading state`() = runTest {
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.user?.email).isEqualTo("jane@example.com")
        assertThat(viewModel.uiState.value.isEmailVerified).isFalse()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `init sets error when current user flow throws`() = runTest {
        every { userRepository.getCurrentUser() } returns flow {
            throw IllegalStateException("Profile load failed")
        }

        viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            signOutUseCase = SignOutUseCase(userRepository, fcmTokenManager),
            deleteAccountUseCase = DeleteAccountUseCase(userRepository),
            sendEmailVerificationUseCase = SendEmailVerificationUseCase(userRepository),
            checkEmailVerificationStatusUseCase = CheckEmailVerificationStatusUseCase(userRepository)
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.error).isEqualTo("Failed to load profile: Profile load failed")
    }

    @Test
    fun `showDeleteConfirmation sets dialog visible`() {
        viewModel.showDeleteConfirmation()

        assertThat(viewModel.uiState.value.showDeleteConfirmation).isTrue()
    }

    @Test
    fun `hideDeleteConfirmation hides dialog`() {
        viewModel.showDeleteConfirmation()

        viewModel.hideDeleteConfirmation()

        assertThat(viewModel.uiState.value.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `deleteAccount success sets account deleted state`() = runTest {
        viewModel.showDeleteConfirmation()

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isDeletingAccount).isFalse()
        assertThat(viewModel.uiState.value.showDeleteConfirmation).isFalse()
        assertThat(viewModel.uiState.value.accountDeleted).isTrue()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `deleteAccount failure uses repository message`() = runTest {
        coEvery {
            userRepository.deleteAccount()
        } returns Result.failure(IllegalStateException("Delete failed"))

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isDeletingAccount).isFalse()
        assertThat(viewModel.uiState.value.accountDeleted).isFalse()
        assertThat(viewModel.uiState.value.error).isEqualTo("Delete failed")
    }

    @Test
    fun `deleteAccount failure without message uses fallback text`() = runTest {
        coEvery { userRepository.deleteAccount() } returns Result.failure(RuntimeException())

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Failed to delete account")
    }

    @Test
    fun `deleteAccount thrown exception uses wrapped message`() = runTest {
        coEvery { userRepository.deleteAccount() } throws IllegalStateException("Delete exploded")

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Failed to delete account: Delete exploded")
        assertThat(viewModel.uiState.value.accountDeleted).isFalse()
    }

    @Test
    fun `deleteAccount ignores duplicate submit while already deleting`() = runTest {
        coEvery { userRepository.deleteAccount() } coAnswers {
            delay(100)
            Result.success(Unit)
        }

        viewModel.deleteAccount()
        viewModel.deleteAccount()
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.deleteAccount() }
    }

    @Test
    fun `sendEmailVerification success sets message`() = runTest {
        viewModel.sendEmailVerification()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSendingVerificationEmail).isFalse()
        assertThat(viewModel.uiState.value.message).isEqualTo("Verification email sent")
        assertThat(viewModel.uiState.value.error).isNull()
        coVerify(exactly = 1) { userRepository.sendEmailVerification() }
    }

    @Test
    fun `sendEmailVerification failure uses repository message`() = runTest {
        coEvery { userRepository.sendEmailVerification() } returns Result.failure(
            IllegalStateException("Verification failed")
        )

        viewModel.sendEmailVerification()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Verification failed")
    }

    @Test
    fun `sendEmailVerification with no user is ignored`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(null)
        viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            signOutUseCase = SignOutUseCase(userRepository, fcmTokenManager),
            deleteAccountUseCase = DeleteAccountUseCase(userRepository),
            sendEmailVerificationUseCase = SendEmailVerificationUseCase(userRepository),
            checkEmailVerificationStatusUseCase = CheckEmailVerificationStatusUseCase(userRepository)
        )
        advanceUntilIdle()

        viewModel.sendEmailVerification()
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.sendEmailVerification() }
        assertThat(viewModel.uiState.value.message).isNull()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `refreshEmailVerificationStatus updates verification flag`() = runTest {
        coEvery { userRepository.isCurrentUserEmailVerified(true) } returns Result.success(true)

        viewModel.refreshEmailVerificationStatus(forceRefresh = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isEmailVerified).isTrue()
        coVerify { userRepository.isCurrentUserEmailVerified(true) }
    }

    @Test
    fun `refreshEmailVerificationStatus failure uses repository message`() = runTest {
        coEvery { userRepository.isCurrentUserEmailVerified(true) } returns Result.failure(
            IllegalStateException("Refresh failed")
        )

        viewModel.refreshEmailVerificationStatus(forceRefresh = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Refresh failed")
    }

    @Test
    fun `clearError removes current error`() = runTest {
        coEvery {
            userRepository.deleteAccount()
        } returns Result.failure(IllegalStateException("Delete failed"))
        viewModel.deleteAccount()
        advanceUntilIdle()

        viewModel.clearError()

        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `clearMessage removes current message`() = runTest {
        viewModel.sendEmailVerification()
        advanceUntilIdle()

        viewModel.clearMessage()

        assertThat(viewModel.uiState.value.message).isNull()
    }
}
