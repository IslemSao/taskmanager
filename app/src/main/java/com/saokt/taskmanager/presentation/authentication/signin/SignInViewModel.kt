package com.saokt.taskmanager.presentation.authentication.signin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.usecase.auth.SignInUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignInWithGoogleUseCase
import com.saokt.taskmanager.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        SignInState(
            email = savedStateHandle[EMAIL_KEY] ?: ""
        )
    )
    val state: StateFlow<SignInState> = _state

    val isUserSignedIn = getCurrentUserUseCase()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onEmailChanged(email: String) {
        savedStateHandle[EMAIL_KEY] = email
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun signIn() {
        val currentState = _state.value
        if (currentState.isLoading) return

        val normalizedEmail = currentState.email.trim()

        if (normalizedEmail.isBlank() || currentState.password.isBlank()) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                error = "Email and password cannot be empty"
            )
            return
        }

        if (!EMAIL_REGEX.matches(normalizedEmail)) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                error = "Enter a valid email address"
            )
            return
        }

        _state.value = currentState.copy(
            email = normalizedEmail,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = signInUseCase(
                email = normalizedEmail,
                password = currentState.password
            )

            if (result.isSuccess) {
                _state.value = currentState.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    isSignedIn = true
                )
            } else {
                _state.value = currentState.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Authentication failed"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (_state.value.isLoading) return

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = signInWithGoogleUseCase(idToken)

            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSignedIn = true
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Google authentication failed"
                )
            }
        }
    }

    fun sendPasswordResetEmail() {
        val currentState = _state.value
        if (currentState.isLoading) return

        val normalizedEmail = currentState.email.trim()
        if (normalizedEmail.isBlank()) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                error = "Enter your email to reset password"
            )
            return
        }

        if (!EMAIL_REGEX.matches(normalizedEmail)) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                error = "Enter a valid email address"
            )
            return
        }

        _state.value = currentState.copy(
            email = normalizedEmail,
            isLoading = true,
            error = null,
            message = null
        )

        viewModelScope.launch {
            val result = sendPasswordResetEmailUseCase(normalizedEmail)
            if (result.isSuccess) {
                _state.value = currentState.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    message = "Password reset email sent"
                )
            } else {
                _state.value = currentState.copy(
                    email = normalizedEmail,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to send password reset email"
                )
            }
        }
    }

    fun setError(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    companion object {
        private const val EMAIL_KEY = "sign_in_email"
        val EMAIL_REGEX: Regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

data class SignInState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
