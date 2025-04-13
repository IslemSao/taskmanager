package com.example.taskmanager.presentation.project.list

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.MainApplication
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.domain.usecase.project.DeleteProjectUseCase
import com.example.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.example.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.example.taskmanager.domain.usecase.task.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val getProjectMembersUseCase : GetProjectMembersUseCase,
    private val application: Application,
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectListState())
    val state: StateFlow<ProjectListState> = _state

    fun loadProjects() {
        (application as MainApplication).applicationScope.launch {
            getProjectsUseCase()
                .onStart {
                    _state.update { it.copy(isLoading = true) }
                }
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "An error occurred while loading projects"
                        )
                    }
                }
                .collect { projects ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            projects = projects,
                            error = null
                        )
                    }
                }
        }
    }

    fun deleteProject(projectId: String) {
        (application as MainApplication).applicationScope.launch {
            deleteProjectUseCase(projectId)
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            error = e.message ?: "u r not the owner of this project!"
                        )
                    }

                }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            getTasksUseCase()
                .collect { tasks ->
                    _state.update { it.copy(tasks = tasks) }
                }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            getProjectMembersUseCase()
                .collect { projectMembers ->
                    _state.update { it.copy(members = projectMembers) }
                    // Handle project members
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class ProjectListState(
    val isLoading: Boolean = false,
    val projects: List<Project> = emptyList(),
    val members : List<ProjectMember> = emptyList(),
    val tasks: List<Task> = emptyList(), // Add tasks to state
    val error: String? = null
)