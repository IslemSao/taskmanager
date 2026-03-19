package com.saokt.taskmanager.presentation.tasks.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectMember
import com.saokt.taskmanager.domain.model.Subtask
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.task.AssignTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.CreateTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdRemoteUseCase
import com.saokt.taskmanager.domain.usecase.task.UpdateTaskUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val assignTaskUseCase: AssignTaskUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val getProjectMembersUseCase: GetProjectMembersUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getTaskByIdRemoteUseCase: GetTaskByIdRemoteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state: StateFlow<TaskDetailState> = _state
    private var taskLoadJob: Job? = null
    private var projectContextJob: Job? = null

    init {
        loadProjects()
        viewModelScope.launch {
            try {
                getCurrentUserUseCase().collect { user ->
                    _state.value = _state.value.copy(currentUser = user)
                }
            } catch (_: Exception) {
                // ignore; current user not critical for screen load
            }
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
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

    fun loadTask(taskId: String, initialProjectId: String? = null) {
        taskLoadJob?.cancel()

        if (taskId == "new") {
            _state.value = TaskDetailState(
                task = Task(
                    title = "",
                    description = "",
                    priority = Priority.MEDIUM,
                    dueDate = null,
                    projectId = initialProjectId
                ),
                isNewTask = true,
                availableProjects = _state.value.availableProjects,
                currentUser = _state.value.currentUser
            )
            if (initialProjectId != null) {
                loadProjectContext(initialProjectId)
            }
            return
        }

        taskLoadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Debug: log identity and incoming id (one-shot)
            val debugUser = try {
                getCurrentUserUseCase().first()
            } catch (_: Exception) { null }
            Log.d("TaskAccessDebug", "loadTask: uid=${debugUser?.id}, taskId=$taskId")

            getTaskByIdUseCase(taskId)
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Unknown error occurred"
                    )
                    Log.e("TaskAccessDebug", "loadTask: error while collecting task id=$taskId: ${e.localizedMessage}")
                }
                .collect { task ->
                    if (task != null) {
                        Log.d("TaskAccessDebug", "loaded local task: id=${task.id} userId=${task.userId} createdBy=${task.createdBy} assignedTo=${task.assignedTo} projectId=${task.projectId}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            task = task,
                            error = null
                        )
                        task.projectId?.let(::loadProjectContext)
                    } else {
                        Log.w("TaskAccessDebug", "local task not found in Room for id=$taskId, trying remote fetch-by-id")
                        try {
                            val res = getTaskByIdRemoteUseCase(taskId)
                            if (res.isSuccess && res.getOrNull() != null) {
                                val fetched = res.getOrNull()!!
                                Log.d("TaskAccessDebug", "remote fetched task: id=${fetched.id}")
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    task = fetched,
                                    error = null
                                )
                                fetched.projectId?.let(::loadProjectContext)
                            } else {
                                Log.w("TaskAccessDebug", "remote fetch failed: ${res.exceptionOrNull()?.message}")
                                _state.value = _state.value.copy(isLoading = false, error = "Task not found")
                            }
                        } catch (e: Exception) {
                            Log.e("TaskAccessDebug", "remote fetch exception", e)
                            _state.value = _state.value.copy(isLoading = false, error = "Task not found")
                        }
                    }
                }
        }
    }

    private fun loadProjectContext(projectId: String) {
        Log.d("TaskCreationDebug", "ViewModel: loadProjectMembers called for $projectId")
        projectContextJob?.cancel()
        projectContextJob = viewModelScope.launch {
            try {
                val project = getProjectByIdUseCase(projectId).first()
                val currentUser = _state.value.currentUser ?: getCurrentUserUseCase().first()
                val members = getProjectMembersUseCase(projectId).first()
                val isOwner = currentUser?.id == project?.ownerId || project?.ownerId.isNullOrBlank()

                _state.value = _state.value.copy(
                    projectMembers = members,
                    isProjectOwner = isOwner
                )
                if (project == null) {
                    _state.value = _state.value.copy(
                        error = "Selected project could not be loaded"
                    )
                }
            } catch (e: Exception) {
                Log.e("TaskCreationDebug", "ViewModel: Error loading project members", e)
                _state.value = _state.value.copy(
                    error = "Failed to load project members: ${e.message}"
                )
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
        
        // Load project members if project changed
        if (projectId != null) {
            loadProjectContext(projectId)
        } else {
            _state.value = _state.value.copy(
                projectMembers = emptyList(),
                isProjectOwner = false
            )
        }
    }

    fun     updateAssignee(assigneeId: String?) {
        val currentTask = _state.value.task
        _state.value = _state.value.copy(
            task = currentTask.copy(assignedTo = assigneeId ,)
        )
    }

    fun assignTask() {
        Log.d("TaskCreationDebug", "ViewModel: assignTask called with ...")
        val currentTask = _state.value.task
        
        currentTask.assignedTo?.let { assigneeId ->
            viewModelScope.launch {
                _state.value = _state.value.copy(isSaving = true)
                
                val currentUser = getCurrentUserUseCase().first()
                if (currentUser == null) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }
                
                val result = assignTaskUseCase(
                    taskId = currentTask.id,
                    assignedToUserId = assigneeId,
                    assignedByUserId = currentUser.id
                )
                
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        task = result.getOrNull() ?: currentTask,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Failed to assign task"
                    )
                }
            }
        }
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
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            val currentTask = _state.value.task
            
            try {
                // Get current user to set createdBy
                val currentUser = getCurrentUserUseCase().first()
                if (currentUser == null) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }
                
                // Set createdBy and assignedTo fields
                val taskWithUser = currentTask.copy(
                    createdBy = currentUser.id,
                    assignedTo = currentTask.assignedTo // Keep existing assignment or null
                )
                
                Log.d("TaskCreationDebug", "ViewModel: saveTask - currentUser.id: ${currentUser.id}")
                Log.d("TaskCreationDebug", "ViewModel: saveTask - taskWithUser: $taskWithUser")
                
                val result = if (_state.value.isNewTask) {
                    createTaskUseCase(taskWithUser)
                } else {
                    updateTaskUseCase(taskWithUser)
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
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.localizedMessage ?: "Failed to save task"
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
    val availableProjects: List<Project> = emptyList(),
    val projectMembers: List<ProjectMember> = emptyList(),
    val isProjectOwner: Boolean = false,
    val currentUser: com.saokt.taskmanager.domain.model.User? = null
)
