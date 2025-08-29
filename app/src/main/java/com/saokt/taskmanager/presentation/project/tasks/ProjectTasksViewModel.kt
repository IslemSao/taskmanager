package com.saokt.taskmanager.presentation.project.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.saokt.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksByProjectFromFirebaseUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskComplitionUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectTasksViewModel @Inject constructor(
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val getProjectMembersUseCase: GetProjectMembersUseCase,
    private val getTasksByProjectFromFirebaseUseCase: GetTasksByProjectFromFirebaseUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskComplitionUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectTasksState())
    val state: StateFlow<ProjectTasksState> = _state.asStateFlow()

    init {
        loadCurrentUser()
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                getProjectByIdUseCase(projectId).collect { project ->
                    Log.d("ProjectTasksDebug", "Project loaded: ${project?.title}")
                    Log.d("ProjectTasksDebug", "Project owner ID: ${project?.ownerId}")
                    Log.d("ProjectTasksDebug", "Project members count: ${project?.members?.size}")
                    project?.members?.forEach { member ->
                        Log.d("ProjectTasksDebug", "Member: ${member.displayName} (${member.userId}), role: ${member.role}")
                    }
                    _state.update { it.copy(project = project, isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("ProjectTasksDebug", "Error loading project", e)
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to load project",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadProjectMembers(projectId: String) {
        viewModelScope.launch {
            try {
                getProjectMembersUseCase(projectId).collect { members ->
                    Log.d("ProjectTasksDebug", "Project members loaded: ${members.size}")
                    members.forEach { member ->
                        Log.d("ProjectTasksDebug", "Member: ${member.displayName} (${member.userId}), role: ${member.role}")
                    }
                    _state.update { it.copy(projectMembers = members) }
                }
            } catch (e: Exception) {
                Log.e("ProjectTasksDebug", "Error loading project members", e)
                _state.update { 
                    it.copy(error = e.message ?: "Failed to load project members")
                }
            }
        }
    }

    fun loadProjectTasks(projectId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProjectTasksDebug", "ViewModel: loadProjectTasks called for projectId: $projectId")
                val result = getTasksByProjectFromFirebaseUseCase(projectId)
                
                if (result.isSuccess) {
                    val tasks = result.getOrNull() ?: emptyList()
                    Log.d("ProjectTasksDebug", "ViewModel: Successfully loaded ${tasks.size} tasks from Firebase")
                    tasks.forEach { task ->
                        Log.d("ProjectTasksDebug", "ViewModel: Task: ${task.title}, userId: ${task.userId}, createdBy: ${task.createdBy}, assignedTo: ${task.assignedTo}")
                    }
                    _state.update { it.copy(tasks = tasks) }
                } else {
                    Log.e("ProjectTasksDebug", "ViewModel: Failed to load tasks: ${result.exceptionOrNull()?.message}")
                    _state.update { 
                        it.copy(error = result.exceptionOrNull()?.message ?: "Failed to load tasks")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProjectTasksDebug", "ViewModel: Exception in loadProjectTasks", e)
                _state.update { 
                    it.copy(error = e.message ?: "Failed to load tasks")
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
                // Reload tasks after deletion
                state.value.project?.let { project ->
                    loadProjectTasks(project.id)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to delete task")
                }
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                toggleTaskCompletionUseCase(task)
                // Reload tasks after toggle
                state.value.project?.let { project ->
                    loadProjectTasks(project.id)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to toggle task completion")
                }
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                Log.d("ProjectTasksDebug", "Current user loaded: ${user?.id}")
                Log.d("ProjectTasksDebug", "Current user email: ${user?.email}")
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class ProjectTasksState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val projectMembers: List<com.saokt.taskmanager.domain.model.ProjectMember> = emptyList(),
    val currentUser: com.saokt.taskmanager.domain.model.User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) 