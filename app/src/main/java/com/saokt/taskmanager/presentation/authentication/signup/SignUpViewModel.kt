package com.saokt.taskmanager.presentation.authentication.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.usecase.auth.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        SignUpState(
            email = savedStateHandle[EMAIL_KEY] ?: "",
            displayName = savedStateHandle[DISPLAY_NAME_KEY] ?: ""
        )
    )
    val state: StateFlow<SignUpState> = _state

    fun onEmailChanged(email: String) {
        savedStateHandle[EMAIL_KEY] = email
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.value = _state.value.copy(confirmPassword = confirmPassword, error = null)
    }

    fun onDisplayNameChanged(displayName: String) {
        savedStateHandle[DISPLAY_NAME_KEY] = displayName
        _state.value = _state.value.copy(displayName = displayName, error = null)
    }

    fun signUp() {
        val currentState = _state.value
        if (currentState.isLoading) return

        val normalizedEmail = currentState.email.trim()
        val normalizedDisplayName = currentState.displayName.trim()

        // Validate inputs
        if (normalizedEmail.isBlank() || currentState.password.isBlank() ||
            currentState.confirmPassword.isBlank() || normalizedDisplayName.isBlank()
        ) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                displayName = normalizedDisplayName,
                error = "All fields are required"
            )
            return
        }

        if (!EMAIL_REGEX.matches(normalizedEmail)) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                displayName = normalizedDisplayName,
                error = "Enter a valid email address"
            )
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                displayName = normalizedDisplayName,
                error = "Passwords do not match"
            )
            return
        }

        if (currentState.password.length < 6) {
            _state.value = currentState.copy(
                email = normalizedEmail,
                displayName = normalizedDisplayName,
                error = "Password must be at least 6 characters"
            )
            return
        }

        _state.value = currentState.copy(
            email = normalizedEmail,
            displayName = normalizedDisplayName,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = signUpUseCase(
                email = normalizedEmail,
                password = currentState.password,
                displayName = normalizedDisplayName
            )

            if (result.isSuccess) {
                _state.value = currentState.copy(
                    email = normalizedEmail,
                    displayName = normalizedDisplayName,
                    isLoading = false,
                    isSignedUp = true
                )
            } else {
                _state.value = currentState.copy(
                    email = normalizedEmail,
                    displayName = normalizedDisplayName,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private companion object {
        private const val EMAIL_KEY = "sign_up_email"
        private const val DISPLAY_NAME_KEY = "sign_up_display_name"
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

data class SignUpState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isSignedUp: Boolean = false,
    val error: String? = null
)
