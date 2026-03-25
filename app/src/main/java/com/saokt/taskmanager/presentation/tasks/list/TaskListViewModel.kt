package com.saokt.taskmanager.presentation.tasks.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.TaskListViewMode
import com.saokt.taskmanager.domain.model.TaskAssignmentFilter
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.TaskPermissionEvaluator
import com.saokt.taskmanager.domain.model.TaskQueryEngine
import com.saokt.taskmanager.domain.model.TaskSort
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
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksUseCase
import com.saokt.taskmanager.domain.usecase.task.RescheduleTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.ResizeTaskScheduleUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskCompletionUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val rescheduleTaskUseCase: RescheduleTaskUseCase,
    private val resizeTaskScheduleUseCase: ResizeTaskScheduleUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val taskPreferencesRepository: TaskPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(TaskListState())
    val state: StateFlow<TaskListState> = _state.asStateFlow()
    private var tasksJob: Job? = null
    private var projectsJob: Job? = null
    private var hasLoadedTasks = false
    private var hasLoadedProjects = false
    private var rawTasks: List<Task> = emptyList()
    private var rawProjects: List<Project> = emptyList()

    init {
        loadTasks()
        loadProjects()
        viewModelScope.launch {
            getCurrentUserUseCase().collectLatest { user ->
                _state.update { it.copy(currentUser = user) }
                recompute()
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeTaskListQuery().collectLatest { query ->
                _state.update { it.copy(activeQuery = query) }
                recompute()
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeTaskListViewMode().collectLatest { mode ->
                _state.update { it.copy(viewMode = mode) }
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeTaskListTimelineZoom().collectLatest { zoom ->
                _state.update { it.copy(timelineZoom = zoom) }
                recompute()
            }
        }
        viewModelScope.launch {
            taskPreferencesRepository.observeTaskListTimelineAnchor().collectLatest { anchorEpochDay ->
                _state.update { it.copy(timelineAnchorEpochDay = anchorEpochDay) }
                recompute()
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val result = toggleTaskCompletionUseCase(task)
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        error = result.exceptionOrNull()?.localizedMessage ?: "Failed to toggle task completion"
                    )
                }
            }
        }
    }

    private fun loadTasks() {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getTasksUseCase()
                .catch { e ->
                    hasLoadedTasks = true
                    _state.update {
                        it.copy(
                            isLoading = shouldShowLoading(),
                            error = e.localizedMessage ?: "Unknown error occurred"
                        )
                    }
                }
                .collectLatest { tasks ->
                    hasLoadedTasks = true
                    rawTasks = tasks
                    _state.update {
                        it.copy(
                            isLoading = shouldShowLoading(),
                            error = null
                        )
                    }
                    recompute()
                }
        }
    }

    private fun loadProjects() {
        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            getProjectsUseCase()
                .catch { e ->
                    hasLoadedProjects = true
                    _state.update {
                        it.copy(
                            isLoading = shouldShowLoading(),
                            error = e.localizedMessage ?: "Failed to load projects"
                        )
                    }
                }
                .collectLatest { projects ->
                    hasLoadedProjects = true
                    rawProjects = projects
                    _state.update {
                        it.copy(
                            projects = projects,
                            isLoading = shouldShowLoading()
                        )
                    }
                    recompute()
                }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = deleteTaskUseCase(taskId)
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        error = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete task"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
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

    fun updateProjectFilter(projectId: String?) = updateQuery { copy(projectId = projectId) }

    fun updateSort(sort: TaskSort) = updateQuery { copy(sort = sort) }

    fun clearFilters() = updateQuery { TaskListQuery(sort = sort) }

    fun setViewMode(mode: TaskListViewMode) {
        _state.update { it.copy(viewMode = mode) }
        viewModelScope.launch {
            taskPreferencesRepository.saveTaskListViewMode(mode)
        }
    }

    fun setTimelineZoom(zoom: TimelineZoom) {
        _state.update { it.copy(timelineZoom = zoom) }
        recompute()
        viewModelScope.launch {
            taskPreferencesRepository.saveTaskListTimelineZoom(zoom)
        }
    }

    fun shiftTimelineAnchor(days: Long) {
        val currentAnchor = _state.value.timelineAnchorDate ?: LocalDate.now()
        val updatedAnchor = currentAnchor.plusDays(days)
        updateTimelineAnchor(updatedAnchor)
    }

    fun jumpTimelineToToday() {
        updateTimelineAnchor(LocalDate.now())
    }

    fun planTaskOnTimeline(task: Task) {
        rescheduleTask(task, 0)
    }

    fun rescheduleTask(task: Task, deltaDays: Long) {
        viewModelScope.launch {
            val result = rescheduleTaskUseCase(task, deltaDays)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.localizedMessage ?: "Failed to reschedule task")
                }
            }
        }
    }

    fun resizeTaskSchedule(task: Task, edge: TimelineEdge, deltaDays: Long) {
        viewModelScope.launch {
            val result = resizeTaskScheduleUseCase(task, edge, deltaDays)
            if (result.isFailure) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.localizedMessage ?: "Failed to resize task")
                }
            }
        }
    }

    private fun updateQuery(transform: TaskListQuery.() -> TaskListQuery) {
        val updatedQuery = _state.value.activeQuery.transform()
        _state.update { it.copy(activeQuery = updatedQuery) }
        recompute()
        viewModelScope.launch {
            taskPreferencesRepository.saveTaskListQuery(updatedQuery)
        }
    }

    private fun recompute() {
        val currentState = _state.value
        val filteredTasks = TaskQueryEngine.apply(
            tasks = rawTasks,
            query = currentState.activeQuery,
            currentUserId = currentState.currentUser?.id
        )
        val timeline = TaskTimelineEngine.build(
            tasks = filteredTasks,
            zoom = currentState.timelineZoom,
            anchorDate = currentState.timelineAnchorDate,
            canEditSchedule = ::canEditSchedule
        )
        _state.update {
            it.copy(
                tasks = filteredTasks,
                filteredTaskCount = filteredTasks.size,
                totalTaskCount = rawTasks.size,
                hasActiveFilters = currentState.activeQuery.hasActiveFilters(),
                timelineRange = timeline.range,
                timelineItems = timeline.items,
                unscheduledTasks = timeline.unscheduledTasks
            )
        }
    }

    private fun shouldShowLoading(): Boolean = !hasLoadedTasks || !hasLoadedProjects

    private fun updateTimelineAnchor(anchorDate: LocalDate) {
        _state.update { it.copy(timelineAnchorEpochDay = anchorDate.toEpochDay()) }
        recompute()
        viewModelScope.launch {
            taskPreferencesRepository.saveTaskListTimelineAnchor(anchorDate.toEpochDay())
        }
    }

    private fun canEditSchedule(task: Task): Boolean {
        val currentUserId = _state.value.currentUser?.id
        val project = rawProjects.firstOrNull { it.id == task.projectId }
        return TaskPermissionEvaluator.canMoveTask(
            task = task,
            currentUserId = currentUserId,
            project = project,
            members = project?.members.orEmpty()
        )
    }
}

data class TaskListState(
    val isLoading: Boolean = false,
    val projects: List<Project> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val currentUser: com.saokt.taskmanager.domain.model.User? = null,
    val activeQuery: TaskListQuery = TaskListQuery(),
    val viewMode: TaskListViewMode = TaskListViewMode.LIST,
    val timelineZoom: TimelineZoom = TimelineZoom.WEEK,
    val timelineAnchorEpochDay: Long? = null,
    val timelineRange: TimelineRange? = null,
    val timelineItems: List<TimelineItem> = emptyList(),
    val unscheduledTasks: List<Task> = emptyList(),
    val filteredTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
    val hasActiveFilters: Boolean = false
) {
    val timelineAnchorDate: LocalDate?
        get() = timelineAnchorEpochDay?.let(LocalDate::ofEpochDay)
}
