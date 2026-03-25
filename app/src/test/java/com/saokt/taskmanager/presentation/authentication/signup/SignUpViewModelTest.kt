package com.saokt.taskmanager.presentation.authentication.signup

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.domain.usecase.auth.SignUpUseCase
import com.saokt.taskmanager.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>()
    private lateinit var viewModel: SignUpViewModel

    @Before
    fun setUp() {
        viewModel = SignUpViewModel(
            signUpUseCase = SignUpUseCase(userRepository),
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `onDisplayNameChanged updates display name`() {
        viewModel.onDisplayNameChanged("Jane Doe")

        assertThat(viewModel.state.value.displayName).isEqualTo("Jane Doe")
    }

    @Test
    fun `onEmailChanged updates email`() {
        viewModel.onEmailChanged("jane@example.com")

        assertThat(viewModel.state.value.email).isEqualTo("jane@example.com")
    }

    @Test
    fun `onPasswordChanged updates password`() {
        viewModel.onPasswordChanged("secret123")

        assertThat(viewModel.state.value.password).isEqualTo("secret123")
    }

    @Test
    fun `onConfirmPasswordChanged updates confirm password`() {
        viewModel.onConfirmPasswordChanged("secret123")

        assertThat(viewModel.state.value.confirmPassword).isEqualTo("secret123")
    }

    @Test
    fun `editing any field clears existing error`() {
        viewModel.signUp()
        assertThat(viewModel.state.value.error).isEqualTo("All fields are required")

        viewModel.onEmailChanged("jane@example.com")
        assertThat(viewModel.state.value.error).isNull()

        viewModel.signUp()
        viewModel.onDisplayNameChanged("Jane Doe")
        assertThat(viewModel.state.value.error).isNull()

        viewModel.signUp()
        viewModel.onPasswordChanged("secret123")
        assertThat(viewModel.state.value.error).isNull()

        viewModel.signUp()
        viewModel.onConfirmPasswordChanged("secret123")
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `signUp with blank fields sets required error and does not call repository`() = runTest {
        viewModel.signUp()

        assertThat(viewModel.state.value.error).isEqualTo("All fields are required")
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isSignedUp).isFalse()
        coVerify(exactly = 0) { userRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `signUp with whitespace only email and name trims and sets required error`() = runTest {
        viewModel.onDisplayNameChanged("   ")
        viewModel.onEmailChanged("   ")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()

        assertThat(viewModel.state.value.displayName).isEmpty()
        assertThat(viewModel.state.value.email).isEmpty()
        assertThat(viewModel.state.value.error).isEqualTo("All fields are required")
        coVerify(exactly = 0) { userRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `signUp with invalid email sets validation error`() = runTest {
        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()

        assertThat(viewModel.state.value.error).isEqualTo("Enter a valid email address")
        assertThat(viewModel.state.value.isLoading).isFalse()
        coVerify(exactly = 0) { userRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `signUp with mismatched passwords sets mismatch error`() = runTest {
        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("different")

        viewModel.signUp()

        assertThat(viewModel.state.value.error).isEqualTo("Passwords do not match")
        assertThat(viewModel.state.value.isLoading).isFalse()
        coVerify(exactly = 0) { userRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `signUp with short password sets length error`() = runTest {
        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("12345")
        viewModel.onConfirmPasswordChanged("12345")

        viewModel.signUp()

        assertThat(viewModel.state.value.error).isEqualTo("Password must be at least 6 characters")
        assertThat(viewModel.state.value.isLoading).isFalse()
        coVerify(exactly = 0) { userRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `signUp success updates signed up state and clears error`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "Jane Doe")
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        } returns Result.success(user)

        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isSignedUp).isTrue()
        assertThat(viewModel.state.value.error).isNull()
        coVerify(exactly = 1) {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        }
    }

    @Test
    fun `signUp trims email and display name before calling repository`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "Jane Doe")
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        } returns Result.success(user)

        viewModel.onDisplayNameChanged("  Jane Doe  ")
        viewModel.onEmailChanged("  jane@example.com  ")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        advanceUntilIdle()

        assertThat(viewModel.state.value.displayName).isEqualTo("Jane Doe")
        assertThat(viewModel.state.value.email).isEqualTo("jane@example.com")
        coVerify(exactly = 1) {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        }
    }

    @Test
    fun `signUp supports non latin display names`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "محمد علي")
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "محمد علي")
        } returns Result.success(user)

        viewModel.onDisplayNameChanged("محمد علي")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isSignedUp).isTrue()
        coVerify(exactly = 1) {
            userRepository.signUp("jane@example.com", "secret123", "محمد علي")
        }
    }

    @Test
    fun `signUp failure uses repository message`() = runTest {
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        } returns Result.failure(IllegalArgumentException("Email already exists"))

        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isSignedUp).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Email already exists")
    }

    @Test
    fun `signUp failure without message uses fallback text`() = runTest {
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        } returns Result.failure(RuntimeException())

        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isSignedUp).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Sign up failed")
    }

    @Test
    fun `signUp can recover from failure and succeed on retry`() = runTest {
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        } returnsMany listOf(
            Result.failure(IllegalStateException("Temporary issue")),
            Result.success(User(id = "u1", email = "jane@example.com", displayName = "Jane Doe"))
        )

        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        advanceUntilIdle()
        assertThat(viewModel.state.value.error).isEqualTo("Temporary issue")
        assertThat(viewModel.state.value.isSignedUp).isFalse()

        viewModel.onEmailChanged("jane@example.com")
        viewModel.signUp()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.isSignedUp).isTrue()
        coVerify(exactly = 2) {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        }
    }

    @Test
    fun `signUp ignores duplicate submit while already loading`() = runTest {
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        } coAnswers {
            delay(100)
            Result.success(User(id = "u1", email = "jane@example.com", displayName = "Jane Doe"))
        }

        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("secret123")
        viewModel.onConfirmPasswordChanged("secret123")

        viewModel.signUp()
        viewModel.signUp()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            userRepository.signUp("jane@example.com", "secret123", "Jane Doe")
        }
    }

    @Test
    fun `clearError removes existing error`() {
        viewModel.onDisplayNameChanged("Jane Doe")
        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("123")
        viewModel.onConfirmPasswordChanged("123")
        viewModel.signUp()

        viewModel.clearError()

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `email and display name are restored from saved state`() {
        val restoredViewModel = SignUpViewModel(
            signUpUseCase = SignUpUseCase(userRepository),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "sign_up_email" to "saved@example.com",
                    "sign_up_display_name" to "Saved Jane"
                )
            )
        )

        assertThat(restoredViewModel.state.value.email).isEqualTo("saved@example.com")
        assertThat(restoredViewModel.state.value.displayName).isEqualTo("Saved Jane")
    }

    @Test
    fun `password fields are not restored from saved state recreation`() {
        val handle = SavedStateHandle()
        val originalViewModel = SignUpViewModel(
            signUpUseCase = SignUpUseCase(userRepository),
            savedStateHandle = handle
        )
        originalViewModel.onEmailChanged("saved@example.com")
        originalViewModel.onDisplayNameChanged("Saved Jane")
        originalViewModel.onPasswordChanged("secret123")
        originalViewModel.onConfirmPasswordChanged("secret123")

        val recreatedViewModel = SignUpViewModel(
            signUpUseCase = SignUpUseCase(userRepository),
            savedStateHandle = handle
        )

        assertThat(recreatedViewModel.state.value.email).isEqualTo("saved@example.com")
        assertThat(recreatedViewModel.state.value.displayName).isEqualTo("Saved Jane")
        assertThat(recreatedViewModel.state.value.password).isEmpty()
        assertThat(recreatedViewModel.state.value.confirmPassword).isEmpty()
    }
}
