package com.saokt.taskmanager.presentation.authentication.signin

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.domain.usecase.auth.SignInUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignInWithGoogleUseCase
import com.saokt.taskmanager.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import com.saokt.taskmanager.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>()
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        every { userRepository.getCurrentUser() } returns flowOf(null)
        viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(userRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(userRepository),
            sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(userRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `onEmailChanged updates email and clears error`() {
        viewModel.setError("Bad credentials")

        viewModel.onEmailChanged("jane@example.com")

        assertThat(viewModel.state.value.email).isEqualTo("jane@example.com")
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `onPasswordChanged updates password and clears error`() {
        viewModel.setError("Bad credentials")

        viewModel.onPasswordChanged("secret123")

        assertThat(viewModel.state.value.password).isEqualTo("secret123")
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `signIn with blank fields sets required error and does not call repository`() = runTest {
        viewModel.signIn()

        assertThat(viewModel.state.value.error).isEqualTo("Email and password cannot be empty")
        coVerify(exactly = 0) { userRepository.signIn(any(), any()) }
    }

    @Test
    fun `signIn with whitespace email trims and sets required error`() = runTest {
        viewModel.onEmailChanged("   ")
        viewModel.onPasswordChanged("secret123")

        viewModel.signIn()

        assertThat(viewModel.state.value.email).isEmpty()
        assertThat(viewModel.state.value.error).isEqualTo("Email and password cannot be empty")
        coVerify(exactly = 0) { userRepository.signIn(any(), any()) }
    }

    @Test
    fun `signIn with invalid email sets validation error`() = runTest {
        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("secret123")

        viewModel.signIn()

        assertThat(viewModel.state.value.error).isEqualTo("Enter a valid email address")
        coVerify(exactly = 0) { userRepository.signIn(any(), any()) }
    }

    @Test
    fun `signIn success trims email and updates signed in state`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "Jane")
        coEvery {
            userRepository.signIn("jane@example.com", "secret123")
        } returns Result.success(user)

        viewModel.onEmailChanged("  jane@example.com  ")
        viewModel.onPasswordChanged("secret123")

        viewModel.signIn()
        advanceUntilIdle()

        assertThat(viewModel.state.value.email).isEqualTo("jane@example.com")
        assertThat(viewModel.state.value.isSignedIn).isTrue()
        assertThat(viewModel.state.value.error).isNull()
        coVerify(exactly = 1) {
            userRepository.signIn("jane@example.com", "secret123")
        }
    }

    @Test
    fun `signIn failure uses repository message`() = runTest {
        coEvery {
            userRepository.signIn("jane@example.com", "secret123")
        } returns Result.failure(IllegalArgumentException("Wrong password"))

        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")

        viewModel.signIn()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isSignedIn).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Wrong password")
    }

    @Test
    fun `signIn failure without message uses fallback text`() = runTest {
        coEvery {
            userRepository.signIn("jane@example.com", "secret123")
        } returns Result.failure(RuntimeException())

        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")

        viewModel.signIn()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo("Authentication failed")
    }

    @Test
    fun `signIn ignores duplicate submit while loading`() = runTest {
        coEvery {
            userRepository.signIn("jane@example.com", "secret123")
        } coAnswers {
            delay(100)
            Result.success(User(id = "u1", email = "jane@example.com"))
        }

        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")

        viewModel.signIn()
        viewModel.signIn()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            userRepository.signIn("jane@example.com", "secret123")
        }
    }

    @Test
    fun `google sign in success updates signed in state`() = runTest {
        coEvery {
            userRepository.signInWithGoogle("token-123")
        } returns Result.success(User(id = "u1", email = "jane@example.com"))

        viewModel.signInWithGoogle("token-123")
        advanceUntilIdle()

        assertThat(viewModel.state.value.isSignedIn).isTrue()
        assertThat(viewModel.state.value.error).isNull()
        coVerify(exactly = 1) {
            userRepository.signInWithGoogle("token-123")
        }
    }

    @Test
    fun `google sign in failure uses repository message when provided`() = runTest {
        coEvery {
            userRepository.signInWithGoogle("token-123")
        } returns Result.failure(IllegalStateException("Google account access denied"))

        viewModel.signInWithGoogle("token-123")
        advanceUntilIdle()

        assertThat(viewModel.state.value.isSignedIn).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Google account access denied")
    }

    @Test
    fun `google sign in failure uses fallback text`() = runTest {
        coEvery {
            userRepository.signInWithGoogle("token-123")
        } returns Result.failure(RuntimeException())

        viewModel.signInWithGoogle("token-123")
        advanceUntilIdle()

        assertThat(viewModel.state.value.isSignedIn).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Google authentication failed")
    }

    @Test
    fun `google sign in ignores duplicate submit while loading`() = runTest {
        coEvery {
            userRepository.signInWithGoogle("token-123")
        } coAnswers {
            delay(100)
            Result.success(User(id = "u1", email = "jane@example.com"))
        }

        viewModel.signInWithGoogle("token-123")
        viewModel.signInWithGoogle("token-123")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            userRepository.signInWithGoogle("token-123")
        }
    }

    @Test
    fun `sendPasswordResetEmail with blank email sets validation error`() = runTest {
        viewModel.sendPasswordResetEmail()

        assertThat(viewModel.state.value.error).isEqualTo("Enter your email to reset password")
        coVerify(exactly = 0) { userRepository.sendPasswordResetEmail(any()) }
    }

    @Test
    fun `sendPasswordResetEmail with invalid email sets validation error`() = runTest {
        viewModel.onEmailChanged("invalid-email")

        viewModel.sendPasswordResetEmail()

        assertThat(viewModel.state.value.error).isEqualTo("Enter a valid email address")
        coVerify(exactly = 0) { userRepository.sendPasswordResetEmail(any()) }
    }

    @Test
    fun `sendPasswordResetEmail success trims email and sets message`() = runTest {
        coEvery { userRepository.sendPasswordResetEmail("jane@example.com") } returns Result.success(Unit)
        viewModel.onEmailChanged("  jane@example.com  ")

        viewModel.sendPasswordResetEmail()
        advanceUntilIdle()

        assertThat(viewModel.state.value.email).isEqualTo("jane@example.com")
        assertThat(viewModel.state.value.message).isEqualTo("Password reset email sent")
        assertThat(viewModel.state.value.error).isNull()
        coVerify(exactly = 1) { userRepository.sendPasswordResetEmail("jane@example.com") }
    }

    @Test
    fun `sendPasswordResetEmail failure uses repository message`() = runTest {
        coEvery {
            userRepository.sendPasswordResetEmail("jane@example.com")
        } returns Result.failure(IllegalStateException("Reset failed"))
        viewModel.onEmailChanged("jane@example.com")

        viewModel.sendPasswordResetEmail()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo("Reset failed")
    }

    @Test
    fun `clearError removes existing error`() {
        viewModel.setError("Wrong password")

        viewModel.clearError()

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `clearMessage removes existing message`() = runTest {
        viewModel.onEmailChanged("jane@example.com")
        coEvery { userRepository.sendPasswordResetEmail("jane@example.com") } returns Result.success(Unit)

        viewModel.sendPasswordResetEmail()
        advanceUntilIdle()

        viewModel.clearMessage()

        assertThat(viewModel.state.value.message).isNull()
    }

    @Test
    fun `email is restored from saved state`() {
        val restoredViewModel = SignInViewModel(
            signInUseCase = SignInUseCase(userRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(userRepository),
            sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(userRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            savedStateHandle = SavedStateHandle(mapOf("sign_in_email" to "saved@example.com"))
        )

        assertThat(restoredViewModel.state.value.email).isEqualTo("saved@example.com")
    }

    @Test
    fun `password is not restored from saved state recreation`() {
        val handle = SavedStateHandle()
        val originalViewModel = SignInViewModel(
            signInUseCase = SignInUseCase(userRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(userRepository),
            sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(userRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            savedStateHandle = handle
        )
        originalViewModel.onEmailChanged("saved@example.com")
        originalViewModel.onPasswordChanged("secret123")

        val recreatedViewModel = SignInViewModel(
            signInUseCase = SignInUseCase(userRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(userRepository),
            sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(userRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            savedStateHandle = handle
        )

        assertThat(recreatedViewModel.state.value.email).isEqualTo("saved@example.com")
        assertThat(recreatedViewModel.state.value.password).isEmpty()
    }
}
