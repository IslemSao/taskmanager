package com.saokt.taskmanager.presentation.project.members

import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        Log.d("ProjectMembersDebug", "loadProjectMembers called with projectId: $projectId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Load project details
                Log.d("ProjectMembersDebug", "Loading project details...")
                getProjectByIdUseCase(projectId).collect { project ->
                    Log.d("ProjectMembersDebug", "Project loaded: ${project?.title}, ownerId: ${project?.ownerId}")
                    Log.d("ProjectMembersDebug", "Project members array: ${project?.members}")
                    if (project != null) {
                        _state.update { 
                            it.copy(
                                project = project,
                                isOwner = project.ownerId == state.value.currentUser?.id
                            )
                        }
                        Log.d("ProjectMembersDebug", "Project state updated, isOwner: ${project.ownerId == state.value.currentUser?.id}")
                    }
                }

                // Load project members
                Log.d("ProjectMembersDebug", "Loading project members from repository...")
                getProjectMembersUseCase(projectId).collect { members ->
                    Log.d("ProjectMembersDebug", "Members loaded from repository: ${members.size} members")
                    members.forEach { member ->
                        Log.d("ProjectMembersDebug", "Member: ${member.displayName} (${member.userId}) - ${member.email}")
                    }
                    _state.update {
                        it.copy(
                            members = members,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d("ProjectMembersDebug", "Members state updated with ${members.size} members")
                }
            } catch (e: Exception) {
                Log.e("ProjectMembersDebug", "Error loading project members", e)
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
                    // Reload members after successful removal
                    loadProjectMembers(projectId)
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