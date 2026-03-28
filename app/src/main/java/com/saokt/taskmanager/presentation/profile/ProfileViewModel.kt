package com.saokt.taskmanager.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.usecase.auth.CheckEmailVerificationStatusUseCase
import com.saokt.taskmanager.domain.usecase.auth.DeleteAccountUseCase
import com.saokt.taskmanager.domain.usecase.auth.SendEmailVerificationUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignOutUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isSendingVerificationEmail: Boolean = false,
    val user: User? = null,
    val isEmailVerified: Boolean? = null,
    val signedOut: Boolean = false,
    val accountDeleted: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val showDeleteConfirmation: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase,
    private val checkEmailVerificationStatusUseCase: CheckEmailVerificationStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                getCurrentUserUseCase().collect { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user
                    )
                    if (user != null) {
                        refreshEmailVerificationStatus(forceRefresh = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load profile: ${e.message}"
                )
            }
        }
    }

    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }

    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }

    fun signOut() {
        if (_uiState.value.isSigningOut || _uiState.value.isDeletingAccount) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSigningOut = true,
                error = null,
                message = null
            )

            val result = signOutUseCase()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSigningOut = false,
                    signedOut = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSigningOut = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to sign out"
                )
            }
        }
    }

    fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = true,
                    showDeleteConfirmation = false,
                    error = null
                )

                val result = deleteAccountUseCase()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isDeletingAccount = false,
                        accountDeleted = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeletingAccount = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete account"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    error = "Failed to delete account: ${e.message}"
                )
            }
        }
    }

    fun sendEmailVerification() {
        if (_uiState.value.isSendingVerificationEmail || _uiState.value.user == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSendingVerificationEmail = true,
                error = null,
                message = null
            )

            val result = sendEmailVerificationUseCase()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSendingVerificationEmail = false,
                    message = "Verification email sent"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSendingVerificationEmail = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to send verification email"
                )
            }
        }
    }

    fun refreshEmailVerificationStatus(forceRefresh: Boolean = true) {
        if (_uiState.value.user == null) return

        viewModelScope.launch {
            val result = checkEmailVerificationStatusUseCase(forceRefresh)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isEmailVerified = result.getOrNull()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to refresh verification status"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
