package com.example.taskmanager.presentation.authentication.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.domain.usecase.auth.SignInUseCase
import com.example.taskmanager.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.taskmanager.domain.usecase.user.GetCurrentUserUseCase
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state

    val isUserSignedIn = getCurrentUserUseCase()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onEmailChanged(email: String) {
        _state.value = _state.value.copy(email = email)
    }

    fun onPasswordChanged(password: String) {
        _state.value = _state.value.copy(password = password)
    }

    fun signIn() {
        val currentState = _state.value

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _state.value = currentState.copy(
                error = "Email and password cannot be empty"
            )
            return
        }

        _state.value = currentState.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val result = signInUseCase(
                email = currentState.email,
                password = currentState.password
            )

            if (result.isSuccess) {
                _state.value = currentState.copy(
                    isLoading = false,
                    isSignedIn = true
                )
            } else {
                _state.value = currentState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Authentication failed"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
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
    fun setError(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class SignInState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val error: String? = null
)
