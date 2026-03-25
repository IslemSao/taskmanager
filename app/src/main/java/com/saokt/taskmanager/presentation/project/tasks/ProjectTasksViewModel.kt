package com.saokt.taskmanager.presentation.project.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectTaskViewMode
import com.saokt.taskmanager.domain.model.TaskAssignmentFilter
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.TaskPermissionEvaluator
import com.saokt.taskmanager.domain.model.TaskQueryEngine
import com.saokt.taskmanager.domain.model.TaskSort
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.TaskStatusFilter
import com.saokt.taskmanager.domain.model.DueDateBucket
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskTimelineEngine
import com.saokt.taskmanager.domain.model.TimelineEdge
import com.saokt.taskmanager.domain.model.TimelineItem
import com.saokt.taskmanager.domain.model.TimelineRange
import com.saokt.taskmanager.domain.model.TimelineZoom
import com.saokt.taskmanager.domain.repository.TaskPreferencesRepository
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.saokt.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksByProjectFromFirebaseUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksByProjectUseCase
import com.saokt.taskmanager.domain.usecase.task.MoveTaskToStatusUseCase
import com.saokt.taskmanager.domain.usecase.task.RescheduleTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.ResizeTaskScheduleUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskCompletionUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ProjectTasksViewModel @Inject constructor(
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val getProjectMembersUseCase: GetProjectMembersUseCase,
    private val getTasksByProjectUseCase: GetTasksByProjectUseCase,
    private val getTasksByProjectFromFirebaseUseCase: GetTasksByProjectFromFirebaseUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val moveTaskToStatusUseCase: MoveTaskToStatusUseCase,
    private val rescheduleTaskUseCase: RescheduleTaskUseCase,
    private val resizeTaskScheduleUseCase: ResizeTaskScheduleUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val taskPreferencesRepository: TaskPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectTasksState())
    val state: StateFlow<ProjectTasksState> = _state.asStateFlow()
    private var projectJob: Job? = null
    private var membersJob: Job? = null
    private var tasksJob: Job? = null
    private var rawTasks: List<Task> = emptyList()

    init {
        loadCurrentUser()
        observePreferences()
    }

    fun loadProject(projectId: String) {
        projectJob?.cancel()
        projectJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                getProjectByIdUseCase(projectId).collectLatest { project ->
                    _state.update {
                        it.copy(
                            project = project,
                            isLoading = false,
                            error = if (project == null) "Project not found" else null
                        )
                    }
                    recompute()
                }
            } catch (e: Exception) {
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
        membersJob?.cancel()
        membersJob = viewModelScope.launch {
            try {
                getProjectMembersUseCase(projectId).collectLatest { members ->
                    _state.update { it.copy(projectMembers = members) }
                    recompute()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Failed to load project members")
                }
            }
        }
    }

    fun loadProjectTasks(projectId: String) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                getTasksByProjectUseCase(projectId).collectLatest { tasks ->
                    rawTasks = tasks
                    _state.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                    recompute()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load tasks",
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            val result = getTasksByProjectFromFirebaseUseCase(projectId)
            if (result.isFailure) {
                _state.update {
                    if (it.tasks.isEmpty()) {
                        it.copy(
                            error = result.exceptionOrNull()?.message ?: "Failed to refresh tasks",
                            isLoading = false
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = deleteTaskUseCase(taskId)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete task")
                }
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val result = toggleTaskCompletionUseCase(task)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to toggle task completion")
                }
            }
        }
    }

    fun moveTask(task: Task, status: TaskStatus) {
        if (!canMoveTask(task)) {
            _state.update { it.copy(error = "You do not have permission to move this task") }
            return
        }
        viewModelScope.launch {
            val result = moveTaskToStatusUseCase(task, status)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to move task")
                }
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collectLatest { user ->
                _state.update { it.copy(currentUser = user) }
                recompute()
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            taskPreferencesRepository.observeProjectTaskQuery().collectLatest { query ->
                _state.update { it.copy(activeQuery = query.copy(projectId = null)) }
                recompute()
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeProjectTaskViewMode().collectLatest { mode ->
                _state.update { it.copy(viewMode = mode) }
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeProjectTaskTimelineZoom().collectLatest { zoom ->
                _state.update { it.copy(timelineZoom = zoom) }
                recompute()
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeProjectTaskTimelineAnchor().collectLatest { anchorEpochDay ->
                _state.update { it.copy(timelineAnchorEpochDay = anchorEpochDay) }
                recompute()
            }
        }
    }

    fun updateStatusFilter(status: TaskStatusFilter) = updateQuery { copy(status = status) }

    fun updateAssignmentFilter(assignment: TaskAssignmentFilter) = updateQuery { copy(assignment = assignment) }

    fun togglePriority(priority: Priority) = updateQuery {
        copy(
            priorities = priorities.toMutableSet().apply {
                if (!add(priority)) {
                    remove(priority)
                }
            }
        )
    }

    fun updateDueDateFilter(dueDateBucket: DueDateBucket) = updateQuery { copy(dueDate = dueDateBucket) }

    fun updateSort(sort: TaskSort) = updateQuery { copy(sort = sort) }

    fun clearFilters() = updateQuery { TaskListQuery(sort = sort) }

    fun setViewMode(mode: ProjectTaskViewMode) {
        _state.update { it.copy(viewMode = mode) }
        viewModelScope.launch {
            taskPreferencesRepository.saveProjectTaskViewMode(mode)
        }
    }

    fun setTimelineZoom(zoom: TimelineZoom) {
        _state.update { it.copy(timelineZoom = zoom) }
        recompute()
        viewModelScope.launch {
            taskPreferencesRepository.saveProjectTaskTimelineZoom(zoom)
        }
    }

    fun shiftTimelineAnchor(days: Long) {
        val currentAnchor = _state.value.timelineAnchorDate ?: LocalDate.now()
        updateTimelineAnchor(currentAnchor.plusDays(days))
    }

    fun jumpTimelineToToday() {
        updateTimelineAnchor(LocalDate.now())
    }

    fun planTaskOnTimeline(task: Task) {
        rescheduleTask(task, 0)
    }

    fun rescheduleTask(task: Task, deltaDays: Long) {
        if (!canMoveTask(task)) {
            _state.update { it.copy(error = "You do not have permission to reschedule this task") }
            return
        }
        viewModelScope.launch {
            val result = rescheduleTaskUseCase(task, deltaDays)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to reschedule task")
                }
            }
        }
    }

    fun resizeTaskSchedule(task: Task, edge: TimelineEdge, deltaDays: Long) {
        if (!canMoveTask(task)) {
            _state.update { it.copy(error = "You do not have permission to resize this task") }
            return
        }
        viewModelScope.launch {
            val result = resizeTaskScheduleUseCase(task, edge, deltaDays)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to resize task")
                }
            }
        }
    }

    private fun updateQuery(transform: TaskListQuery.() -> TaskListQuery) {
        val updated = _state.value.activeQuery.transform().copy(projectId = null)
        _state.update { it.copy(activeQuery = updated) }
        recompute()
        viewModelScope.launch {
            taskPreferencesRepository.saveProjectTaskQuery(updated)
        }
    }

    private fun recompute() {
        val state = _state.value
        val filteredTasks = TaskQueryEngine.apply(
            tasks = rawTasks,
            query = state.activeQuery.copy(projectId = null),
            currentUserId = state.currentUser?.id
        )
        val grouped = TaskQueryEngine.groupByStatus(filteredTasks)
        val movable = filteredTasks.associate { task -> task.id to canMoveTask(task) }
        val timeline = TaskTimelineEngine.build(
            tasks = filteredTasks,
            zoom = state.timelineZoom,
            anchorDate = state.timelineAnchorDate,
            canEditSchedule = ::canMoveTask
        )
        _state.update {
            it.copy(
                tasks = filteredTasks,
                groupedTasks = grouped,
                canMoveTaskById = movable,
                timelineRange = timeline.range,
                timelineItems = timeline.items,
                unscheduledTasks = timeline.unscheduledTasks,
                filteredTaskCount = filteredTasks.size,
                totalTaskCount = rawTasks.size,
                hasActiveFilters = state.activeQuery.hasActiveFilters()
            )
        }
    }

    private fun canMoveTask(task: Task): Boolean {
        return TaskPermissionEvaluator.canMoveTask(
            task = task,
            currentUserId = _state.value.currentUser?.id,
            project = _state.value.project,
            members = _state.value.projectMembers
        )
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun updateTimelineAnchor(anchorDate: LocalDate) {
        _state.update { it.copy(timelineAnchorEpochDay = anchorDate.toEpochDay()) }
        recompute()
        viewModelScope.launch {
            taskPreferencesRepository.saveProjectTaskTimelineAnchor(anchorDate.toEpochDay())
        }
    }
}

data class ProjectTasksState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val projectMembers: List<com.saokt.taskmanager.domain.model.ProjectMember> = emptyList(),
    val currentUser: com.saokt.taskmanager.domain.model.User? = null,
    val activeQuery: TaskListQuery = TaskListQuery(),
    val viewMode: ProjectTaskViewMode = ProjectTaskViewMode.LIST,
    val timelineZoom: TimelineZoom = TimelineZoom.WEEK,
    val timelineAnchorEpochDay: Long? = null,
    val groupedTasks: Map<TaskStatus, List<Task>> = emptyMap(),
    val timelineRange: TimelineRange? = null,
    val timelineItems: List<TimelineItem> = emptyList(),
    val unscheduledTasks: List<Task> = emptyList(),
    val canMoveTaskById: Map<String, Boolean> = emptyMap(),
    val filteredTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
    val hasActiveFilters: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val timelineAnchorDate: LocalDate?
        get() = timelineAnchorEpochDay?.let(LocalDate::ofEpochDay)
}
