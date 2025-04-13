package com.example.taskmanager.presentation.project.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.model.User
import com.example.taskmanager.domain.usecase.project.CreateProjectUseCase
import com.example.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.example.taskmanager.domain.usecase.project.InviteProjectMemberUseCase
import com.example.taskmanager.domain.usecase.project.RemoveProjectMemberUseCase
import com.example.taskmanager.domain.usecase.project.UpdateProjectUseCase
import com.example.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectByIdUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val inviteMemberUseCase: InviteProjectMemberUseCase,
    private val removeMemberUseCase: RemoveProjectMemberUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    data class ProjectDetailState(
        val project: Project = Project(title = ""),
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val isNewProject: Boolean = true,
        val isProjectSaved: Boolean = false,
        val error: String? = null,
        val inviteEmail: String = "",
        val isInviting: Boolean = false,
        val inviteError: String? = null,
        val currentUser: User? = null,
        val pendingInvites: List<String> = emptyList()
    )

    private val _state = MutableStateFlow(ProjectDetailState())
    val state: StateFlow<ProjectDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    fun initializeNewProject() {
        val currentUserId = _state.value.currentUser?.id ?: ""
        val userDisplayName = _state.value.currentUser?.displayName ?: _state.value.currentUser?.email ?: ""
        val userEmail = _state.value.currentUser?.email ?: ""

        val owner = ProjectMember(
            projectId = "",
            userId = currentUserId,
            email = userEmail,
            displayName = userDisplayName,
            role = com.example.taskmanager.domain.model.ProjectRole.OWNER
        )

        _state.update {
            it.copy(
                project = Project(
                    title = "",
                    ownerId = currentUserId,
                    members = listOf(owner)
                ),
                isNewProject = true
            )
        }
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val projectFlow = getProjectUseCase(projectId)
                projectFlow.collect { project ->
                    if (project != null) {
                        _state.update {
                            it.copy(
                                project = project,
                                isLoading = false,
                                isNewProject = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Project not found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading project: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _state.update {
            it.copy(project = it.project.copy(title = title))
        }
    }

    fun updateDescription(description: String) {
        _state.update {
            it.copy(project = it.project.copy(description = description))
        }
    }

    fun updateColor(color: Int) {
        _state.update {
            it.copy(project = it.project.copy(color = color))
        }
    }

    fun updateStartDate(date: Date) {
        _state.update {
            it.copy(project = it.project.copy(startDate = date))
        }
    }

    fun updateDueDate(date: Date) {
        _state.update {
            it.copy(project = it.project.copy(dueDate = date))
        }
    }

    fun updateInviteEmail(email: String) {
        _state.update { it.copy(inviteEmail = email, inviteError = null) }
    }

    fun addPendingInvite() {
        val email = _state.value.inviteEmail.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update {
                it.copy(inviteError = "Invalid email format")
            }
            return
        }

        if (_state.value.pendingInvites.contains(email)) {
            _state.update {
                it.copy(
                    inviteError = "This email is already in the invite list",
                    inviteEmail = ""
                )
            }
            return
        }

        _state.update {
            it.copy(
                pendingInvites = it.pendingInvites + email,
                inviteEmail = "",
                inviteError = null
            )
        }
    }

    fun removePendingInvite(email: String) {
        _state.update {
            it.copy(
                pendingInvites = it.pendingInvites.filter { e -> e != email }
            )
        }
    }

    fun inviteMember() {
        viewModelScope.launch {
            Log.d("invitation", "inviteMember vm fun")
            _state.update { it.copy(isInviting = true, inviteError = null) }

            val result = inviteMemberUseCase(
                projectId = state.value.project.id,
                projectTitle = state.value.project.title,
                userEmail = state.value.inviteEmail
            )

            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        isInviting = false,
                        inviteEmail = "",
                        inviteError = null
                    )
                }
                loadProject(state.value.project.id)
            } else {
                _state.update {
                    it.copy(
                        isInviting = false,
                        inviteError = result.exceptionOrNull()?.message ?: "Failed to invite member"
                    )
                }
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            val result = removeMemberUseCase(
                projectId = state.value.project.id,
                userId = userId
            )

            if (result.isSuccess) {
                loadProject(state.value.project.id)
            } else {
                _state.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to remove member"
                    )
                }
            }
        }
    }

    fun saveProject() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            try {
                val result = if (_state.value.isNewProject) {
                    createProjectUseCase(_state.value.project)
                } else {
                    updateProjectUseCase(_state.value.project)
                }

                if (result.isSuccess) {
                    // For new projects, send invitations for all pending invites
                    if (_state.value.isNewProject && _state.value.pendingInvites.isNotEmpty()) {
                        val projectId = result.getOrNull()?.id ?: ""
                        val projectTitle = result.getOrNull()?.title ?: ""

                        for (email in _state.value.pendingInvites) {
                            inviteMemberUseCase(projectId , projectTitle, email)
                        }
                    }

                    _state.update {
                        it.copy(
                            isSaving = false,
                            isProjectSaved = true
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save project"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Error saving project: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
