package com.saokt.taskmanager.presentation.tasks.list

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskAssignmentFilter
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.TaskListViewMode
import com.saokt.taskmanager.domain.model.TimelineEdge
import com.saokt.taskmanager.domain.model.TimelineZoom
import com.saokt.taskmanager.domain.repository.TaskPreferencesRepository
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksUseCase
import com.saokt.taskmanager.domain.usecase.task.RescheduleTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.ResizeTaskScheduleUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskCompletionUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import com.saokt.taskmanager.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTasksUseCase = mockk<GetTasksUseCase>()
    private val getProjectsUseCase = mockk<GetProjectsUseCase>()
    private val deleteTaskUseCase = mockk<DeleteTaskUseCase>(relaxed = true)
    private val rescheduleTaskUseCase = mockk<RescheduleTaskUseCase>(relaxed = true)
    private val resizeTaskScheduleUseCase = mockk<ResizeTaskScheduleUseCase>(relaxed = true)
    private val toggleTaskCompletionUseCase = mockk<ToggleTaskCompletionUseCase>(relaxed = true)
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val preferencesRepository = FakeTaskPreferencesRepository()

    @Test
    fun `applies persisted query to task list`() = runTest {
        every { getTasksUseCase.invoke() } returns flowOf(
            listOf(
                Task(id = "a", title = "Mine", assignedTo = "u1", priority = Priority.HIGH),
                Task(id = "b", title = "Other", assignedTo = "u2", priority = Priority.LOW)
            )
        )
        every { getProjectsUseCase.invoke() } returns flowOf(emptyList())
        every { getCurrentUserUseCase.invoke() } returns flowOf(com.saokt.taskmanager.domain.model.User(id = "u1", email = "u1@example.com"))
        preferencesRepository.taskListQuery.value = TaskListQuery(assignment = TaskAssignmentFilter.ASSIGNED_TO_ME)

        val viewModel = TaskListViewModel(
            getTasksUseCase = getTasksUseCase,
            getProjectsUseCase = getProjectsUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            rescheduleTaskUseCase = rescheduleTaskUseCase,
            resizeTaskScheduleUseCase = resizeTaskScheduleUseCase,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            taskPreferencesRepository = preferencesRepository
        )

        advanceUntilIdle()

        assertThat(viewModel.state.value.tasks.map { it.id }).containsExactly("a")
        assertThat(viewModel.state.value.filteredTaskCount).isEqualTo(1)
        assertThat(viewModel.state.value.totalTaskCount).isEqualTo(2)
    }

    @Test
    fun `restores timeline preferences for task list`() = runTest {
        every { getTasksUseCase.invoke() } returns flowOf(listOf(Task(id = "a", title = "Scheduled")))
        every { getProjectsUseCase.invoke() } returns flowOf(emptyList())
        every { getCurrentUserUseCase.invoke() } returns flowOf(com.saokt.taskmanager.domain.model.User(id = "u1", email = "u1@example.com"))
        preferencesRepository.taskListViewMode.value = TaskListViewMode.TIMELINE
        preferencesRepository.taskListZoom.value = TimelineZoom.MONTH
        preferencesRepository.taskListAnchor.value = 20_000L

        val viewModel = TaskListViewModel(
            getTasksUseCase = getTasksUseCase,
            getProjectsUseCase = getProjectsUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            rescheduleTaskUseCase = rescheduleTaskUseCase,
            resizeTaskScheduleUseCase = resizeTaskScheduleUseCase,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            taskPreferencesRepository = preferencesRepository
        )

        advanceUntilIdle()

        assertThat(viewModel.state.value.viewMode).isEqualTo(TaskListViewMode.TIMELINE)
        assertThat(viewModel.state.value.timelineZoom).isEqualTo(TimelineZoom.MONTH)
        assertThat(viewModel.state.value.timelineAnchorEpochDay).isEqualTo(20_000L)
    }
}

private class FakeTaskPreferencesRepository : TaskPreferencesRepository {
    val taskListQuery = MutableStateFlow(TaskListQuery())
    val taskListViewMode = MutableStateFlow(TaskListViewMode.LIST)
    val taskListZoom = MutableStateFlow(TimelineZoom.WEEK)
    val taskListAnchor = MutableStateFlow<Long?>(null)
    private val projectTaskQuery = MutableStateFlow(TaskListQuery())
    private val projectTaskViewMode = MutableStateFlow(com.saokt.taskmanager.domain.model.ProjectTaskViewMode.LIST)

    override fun observeTaskListQuery(): Flow<TaskListQuery> = taskListQuery

    override suspend fun saveTaskListQuery(query: TaskListQuery) {
        taskListQuery.value = query
    }

    override fun observeTaskListViewMode(): Flow<TaskListViewMode> = taskListViewMode

    override suspend fun saveTaskListViewMode(mode: TaskListViewMode) {
        taskListViewMode.value = mode
    }

    override fun observeTaskListTimelineZoom(): Flow<TimelineZoom> = taskListZoom

    override suspend fun saveTaskListTimelineZoom(zoom: TimelineZoom) {
        taskListZoom.value = zoom
    }

    override fun observeTaskListTimelineAnchor(): Flow<Long?> = taskListAnchor

    override suspend fun saveTaskListTimelineAnchor(anchorEpochDay: Long?) {
        taskListAnchor.value = anchorEpochDay
    }

    override fun observeProjectTaskQuery(): Flow<TaskListQuery> = projectTaskQuery

    override suspend fun saveProjectTaskQuery(query: TaskListQuery) {
        projectTaskQuery.value = query
    }

    override fun observeProjectTaskViewMode(): Flow<com.saokt.taskmanager.domain.model.ProjectTaskViewMode> = projectTaskViewMode

    override suspend fun saveProjectTaskViewMode(mode: com.saokt.taskmanager.domain.model.ProjectTaskViewMode) {
        projectTaskViewMode.value = mode
    }
}
