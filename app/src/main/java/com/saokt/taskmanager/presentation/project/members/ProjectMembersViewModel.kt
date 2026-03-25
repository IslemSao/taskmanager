package com.saokt.taskmanager.presentation.project.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectMember
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.saokt.taskmanager.domain.usecase.project.RemoveProjectMemberUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectMembersViewModel @Inject constructor(
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val getProjectMembersUseCase: GetProjectMembersUseCase,
    private val removeProjectMemberUseCase: RemoveProjectMemberUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectMembersState())
    val state: StateFlow<ProjectMembersState> = _state.asStateFlow()
    private var projectMembersJob: Job? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    fun loadProjectMembers(projectId: String) {
        projectMembersJob?.cancel()
        projectMembersJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val project = getProjectByIdUseCase(projectId).first()
                if (project == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Project not found"
                        )
                    }
                    return@launch
                }

                _state.update {
                    it.copy(
                        project = project,
                        isOwner = project.ownerId == _state.value.currentUser?.id
                    )
                }

                getProjectMembersUseCase(projectId).collectLatest { members ->
                    _state.update {
                        it.copy(
                            members = members,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load project members: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeMember(userId: String) {
        val projectId = state.value.project?.id ?: return
        
        viewModelScope.launch {
            try {
                val result = removeProjectMemberUseCase(projectId, userId)
                if (result.isSuccess) {
                    _state.update { currentState ->
                        currentState.copy(
                            members = currentState.members.filterNot { it.userId == userId },
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            error = result.exceptionOrNull()?.message ?: "Failed to remove member"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Error removing member: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class ProjectMembersState(
    val isLoading: Boolean = false,
    val project: Project? = null,
    val members: List<ProjectMember> = emptyList(),
    val currentUser: User? = null,
    val isOwner: Boolean = false,
    val error: String? = null
) 
