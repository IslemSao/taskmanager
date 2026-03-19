package com.saokt.taskmanager.presentation.invitation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.ProjectInvitation
import com.saokt.taskmanager.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvitationsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    data class InvitationsState(
        val invitations: List<ProjectInvitation> = emptyList(),
        val isLoading: Boolean = false,
        val processingInvitationId: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(InvitationsState())
    val state: StateFlow<InvitationsState> = _state.asStateFlow()

    init {
        loadInvitations()
    }

    private fun loadInvitations() {
        viewModelScope.launch {

            _state.update { it.copy(isLoading = true) }

            try {
                projectRepository.getProjectInvitations().collect { invitations ->
                    _state.update {
                        it.copy(
                            invitations = invitations.filter { inv -> inv.status == com.saokt.taskmanager.domain.model.InvitationStatus.PENDING },
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load invitations: ${e.message}"
                    )
                }
            }
        }
    }

    fun respondToInvitation(invitationId: String, accept: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    processingInvitationId = invitationId,
                    error = null
                )
            }

            try {
                val result = projectRepository.respondToInvitation(invitationId, accept)

                if (result.isFailure) {
                    _state.update {
                        it.copy(
                            processingInvitationId = null,
                            error = "Failed to respond to invitation: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    _state.update { it.copy(processingInvitationId = null) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        processingInvitationId = null,
                        error = "Failed to respond to invitation: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
