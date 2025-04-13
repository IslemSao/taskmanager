package com.example.taskmanager.presentation.tasks.detail

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.MainApplication
import com.example.taskmanager.domain.model.Priority
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.Subtask
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.example.taskmanager.domain.usecase.task.CreateTaskUseCase
import com.example.taskmanager.domain.usecase.task.GetTaskByIdUseCase
import com.example.taskmanager.domain.usecase.task.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state: StateFlow<TaskDetailState> = _state

    init {
        loadProjects()
    }

    private fun loadProjects() {
        (application as MainApplication).applicationScope.launch {
            try {
                getProjectsUseCase().collect { projects ->
                    _state.value = _state.value.copy(
                        availableProjects = projects
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load projects: ${e.message}"
                )
            }
        }
    }

    fun loadTask(taskId: String) {
        if (taskId == "new") {
            _state.value = TaskDetailState(
                task = Task(
                    title = "",
                    description = "",
                    priority = Priority.MEDIUM,
                    dueDate = null
                ),
                isNewTask = true,
                availableProjects = _state.value.availableProjects
            )
            return
        }

        (application as MainApplication).applicationScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            getTaskByIdUseCase(taskId)
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Unknown error occurred"
                    )
                }
                .collect { task ->
                    if (task != null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            task = task,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Task not found"
                        )
                    }
                }
        }
    }

    fun updateTitle(title: String) {
        val currentTask = _state.value.task
        _state.value = _state.value.copy(
            task = currentTask.copy(title = title)
        )
    }

    fun updateDescription(description: String) {
        val currentTask = _state.value.task
        _state.value = _state.value.copy(
            task = currentTask.copy(description = description)
        )
    }

    fun updateDueDate(dueDate: Date?) {
        val currentTask = _state.value.task
        _state.value = _state.value.copy(
            task = currentTask.copy(dueDate = dueDate)
        )
    }

    fun updatePriority(priority: Priority) {
        val currentTask = _state.value.task
        _state.value = _state.value.copy(
            task = currentTask.copy(priority = priority)
        )
    }

    fun updateProject(projectId: String?) {
        val currentTask = _state.value.task
        _state.value = _state.value.copy(
            task = currentTask.copy(projectId = projectId)
        )
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return

        val currentTask = _state.value.task
        val newSubtask = Subtask(title = title)
        val updatedSubtasks = currentTask.subtasks + newSubtask

        _state.value = _state.value.copy(
            task = currentTask.copy(subtasks = updatedSubtasks)
        )
    }

    fun removeSubtask(subtaskId: String) {
        val currentTask = _state.value.task
        val updatedSubtasks = currentTask.subtasks.filter { it.id != subtaskId }

        _state.value = _state.value.copy(
            task = currentTask.copy(subtasks = updatedSubtasks)
        )
    }

    fun toggleSubtaskCompletion(subtaskId: String) {
        val currentTask = _state.value.task
        val updatedSubtasks = currentTask.subtasks.map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }

        _state.value = _state.value.copy(
            task = currentTask.copy(subtasks = updatedSubtasks)
        )
    }

    fun saveTask() {
        (application as MainApplication).applicationScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            val currentTask = _state.value.task
            val result = if (_state.value.isNewTask) {
                createTaskUseCase(currentTask)
            } else {
                updateTaskUseCase(currentTask)
            }

            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    isTaskSaved = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.localizedMessage ?: "Failed to save task"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class TaskDetailState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val task: Task = Task(title = ""),
    val isNewTask: Boolean = false,
    val isTaskSaved: Boolean = false,
    val error: String? = null,
    val availableProjects: List<Project> = emptyList()
)
