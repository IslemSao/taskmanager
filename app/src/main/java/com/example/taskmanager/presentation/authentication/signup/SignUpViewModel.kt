package com.example.taskmanager.presentation.authentication.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state

    fun onEmailChanged(email: String) {
        _state.value = _state.value.copy(email = email)
    }

    fun onPasswordChanged(password: String) {
        _state.value = _state.value.copy(password = password)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.value = _state.value.copy(confirmPassword = confirmPassword)
    }

    fun onDisplayNameChanged(displayName: String) {
        _state.value = _state.value.copy(displayName = displayName)
    }

    fun signUp() {
        val currentState = _state.value

        // Validate inputs
        if (currentState.email.isBlank() || currentState.password.isBlank() ||
            currentState.confirmPassword.isBlank() || currentState.displayName.isBlank()
        ) {
            _state.value = currentState.copy(
                error = "All fields are required"
            )
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.value = currentState.copy(
                error = "Passwords do not match"
            )
            return
        }

        if (currentState.password.length < 6) {
            _state.value = currentState.copy(
                error = "Password must be at least 6 characters"
            )
            return
        }

        _state.value = currentState.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = userRepository.signUp(
                email = currentState.email,
                password = currentState.password,
                displayName = currentState.displayName
            )

            if (result.isSuccess) {
                _state.value = currentState.copy(
                    isLoading = false,
                    isSignedUp = true
                )
            } else {
                _state.value = currentState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
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
