package com.example.taskmanager.presentation.tasks.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.example.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.example.taskmanager.domain.usecase.task.GetTasksUseCase
import com.example.taskmanager.domain.usecase.task.ToggleTaskComplitionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Import asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update // Import update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleTaskComplitionUseCae: ToggleTaskComplitionUseCase
) : ViewModel() {

    // Use asStateFlow() for the public state
    private val _state = MutableStateFlow(TaskListState())
    val state: StateFlow<TaskListState> = _state.asStateFlow()

    init {
        loadTasks()
        loadProjects()
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val result = toggleTaskComplitionUseCae(task)
            if (result.isFailure) {
                Log.e("DashboardVM", "Failed to toggle task completion", result.exceptionOrNull())
                _state.update { it.copy(error = "Failed to toggle task completion") }
            }
        }
    }

    fun loadTasks() { // Keep projectId param if needed later
        viewModelScope.launch {
            // Use update for cleaner state modification
            _state.update { it.copy(isLoading = true) }

            getTasksUseCase()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Unknown error occurred"
                        )
                    }
                }
                .collect { tasks ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks,
                            error = null
                        )
                    }
                }

        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getProjectsUseCase()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Failed to load projects"
                        )
                    }
                }
                .collect { projects ->
                    _state.update { currentState ->
                        currentState.copy(
                            projects = projects,
                            isLoading = currentState.tasks.isEmpty() // Keep loading if tasks not loaded yet
                        )
                    }
                }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            // Optional: Don't show full loading state for delete
            // _state.update { it.copy(isLoading = true) } // Consider removing this for delete
            val result = deleteTaskUseCase(taskId)

            // The flow from getTasksUseCase will automatically update the list
            // upon successful deletion in the repository.
            // We just need to handle the error case.

            if (result.isFailure) {
                _state.update {
                    it.copy(
                        // isLoading = false, // Remove if you removed the start loading state
                        error = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete task"
                    )
                }
            }
            // No need to explicitly set isLoading = false if you didn't set it to true,
            // or update tasks list here, as the flow handles it.
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

// Make Task list explicitly typed
data class TaskListState(
    val isLoading: Boolean = false,
    val projects : List<Project> = emptyList(),
    val tasks: List<Task> = emptyList(), // Use List<Task>
    val error: String? = null
)