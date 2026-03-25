package com.saokt.taskmanager.presentation.project.tasks

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectTaskViewMode
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
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
class ProjectTasksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getProjectByIdUseCase = mockk<GetProjectByIdUseCase>()
    private val getProjectMembersUseCase = mockk<GetProjectMembersUseCase>()
    private val getTasksByProjectUseCase = mockk<GetTasksByProjectUseCase>()
    private val getTasksByProjectFromFirebaseUseCase = mockk<GetTasksByProjectFromFirebaseUseCase>()
    private val deleteTaskUseCase = mockk<DeleteTaskUseCase>(relaxed = true)
    private val moveTaskToStatusUseCase = mockk<MoveTaskToStatusUseCase>(relaxed = true)
    private val rescheduleTaskUseCase = mockk<RescheduleTaskUseCase>(relaxed = true)
    private val resizeTaskScheduleUseCase = mockk<ResizeTaskScheduleUseCase>(relaxed = true)
    private val toggleTaskCompletionUseCase = mockk<ToggleTaskCompletionUseCase>(relaxed = true)
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val preferencesRepository = FakeProjectTaskPreferencesRepository()

    @Test
    fun `groups tasks for board view and restores preferred mode`() = runTest {
        every { getCurrentUserUseCase.invoke() } returns flowOf(com.saokt.taskmanager.domain.model.User(id = "owner", email = "owner@example.com"))
        every { getProjectByIdUseCase.invoke("p1") } returns flowOf(Project(id = "p1", title = "Showcase", ownerId = "owner"))
        coEvery { getProjectMembersUseCase.invoke("p1") } returns flowOf(emptyList())
        every { getTasksByProjectUseCase.invoke("p1") } returns flowOf(
            listOf(
                Task(id = "todo", title = "Todo", status = TaskStatus.TODO),
                Task(id = "doing", title = "Doing", status = TaskStatus.IN_PROGRESS),
                Task(id = "done", title = "Done", completed = true, status = TaskStatus.DONE)
            )
        )
        coEvery { getTasksByProjectFromFirebaseUseCase.invoke("p1") } returns Result.success(emptyList())
        preferencesRepository.viewMode.value = ProjectTaskViewMode.BOARD

        val viewModel = ProjectTasksViewModel(
            getProjectByIdUseCase = getProjectByIdUseCase,
            getProjectMembersUseCase = getProjectMembersUseCase,
            getTasksByProjectUseCase = getTasksByProjectUseCase,
            getTasksByProjectFromFirebaseUseCase = getTasksByProjectFromFirebaseUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            moveTaskToStatusUseCase = moveTaskToStatusUseCase,
            rescheduleTaskUseCase = rescheduleTaskUseCase,
            resizeTaskScheduleUseCase = resizeTaskScheduleUseCase,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            taskPreferencesRepository = preferencesRepository
        )

        viewModel.loadProject("p1")
        viewModel.loadProjectMembers("p1")
        viewModel.loadProjectTasks("p1")
        advanceUntilIdle()

        assertThat(viewModel.state.value.viewMode).isEqualTo(ProjectTaskViewMode.BOARD)
        assertThat(viewModel.state.value.groupedTasks[TaskStatus.TODO]?.map { it.id }).containsExactly("todo")
        assertThat(viewModel.state.value.groupedTasks[TaskStatus.IN_PROGRESS]?.map { it.id }).containsExactly("doing")
        assertThat(viewModel.state.value.groupedTasks[TaskStatus.DONE]?.map { it.id }).containsExactly("done")
    }

    @Test
    fun `restores project timeline zoom preference`() = runTest {
        every { getCurrentUserUseCase.invoke() } returns flowOf(com.saokt.taskmanager.domain.model.User(id = "owner", email = "owner@example.com"))
        every { getProjectByIdUseCase.invoke("p1") } returns flowOf(Project(id = "p1", title = "Showcase", ownerId = "owner"))
        coEvery { getProjectMembersUseCase.invoke("p1") } returns flowOf(emptyList())
        every { getTasksByProjectUseCase.invoke("p1") } returns flowOf(
            listOf(
                Task(
                    id = "todo",
                    title = "Timeline task",
                    dueDate = java.util.Date(1_711_584_000_000L)
                )
            )
        )
        coEvery { getTasksByProjectFromFirebaseUseCase.invoke("p1") } returns Result.success(emptyList())
        preferencesRepository.viewMode.value = ProjectTaskViewMode.TIMELINE
        preferencesRepository.projectZoom.value = TimelineZoom.MONTH

        val viewModel = ProjectTasksViewModel(
            getProjectByIdUseCase = getProjectByIdUseCase,
            getProjectMembersUseCase = getProjectMembersUseCase,
            getTasksByProjectUseCase = getTasksByProjectUseCase,
            getTasksByProjectFromFirebaseUseCase = getTasksByProjectFromFirebaseUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            moveTaskToStatusUseCase = moveTaskToStatusUseCase,
            rescheduleTaskUseCase = rescheduleTaskUseCase,
            resizeTaskScheduleUseCase = resizeTaskScheduleUseCase,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            taskPreferencesRepository = preferencesRepository
        )

        viewModel.loadProject("p1")
        viewModel.loadProjectMembers("p1")
        viewModel.loadProjectTasks("p1")
        advanceUntilIdle()

        assertThat(viewModel.state.value.viewMode).isEqualTo(ProjectTaskViewMode.TIMELINE)
        assertThat(viewModel.state.value.timelineZoom).isEqualTo(TimelineZoom.MONTH)
        assertThat(viewModel.state.value.timelineItems).hasSize(1)
    }
}

private class FakeProjectTaskPreferencesRepository : TaskPreferencesRepository {
    private val taskListQuery = MutableStateFlow(com.saokt.taskmanager.domain.model.TaskListQuery())
    private val projectTaskQuery = MutableStateFlow(com.saokt.taskmanager.domain.model.TaskListQuery())
    val viewMode = MutableStateFlow(ProjectTaskViewMode.LIST)
    val projectZoom = MutableStateFlow(TimelineZoom.WEEK)
    val projectAnchor = MutableStateFlow<Long?>(null)

    override fun observeTaskListQuery(): Flow<com.saokt.taskmanager.domain.model.TaskListQuery> = taskListQuery

    override suspend fun saveTaskListQuery(query: com.saokt.taskmanager.domain.model.TaskListQuery) {
        taskListQuery.value = query
    }

    override fun observeProjectTaskQuery(): Flow<com.saokt.taskmanager.domain.model.TaskListQuery> = projectTaskQuery

    override suspend fun saveProjectTaskQuery(query: com.saokt.taskmanager.domain.model.TaskListQuery) {
        projectTaskQuery.value = query
    }

    override fun observeProjectTaskViewMode(): Flow<ProjectTaskViewMode> = viewMode

    override suspend fun saveProjectTaskViewMode(mode: ProjectTaskViewMode) {
        viewMode.value = mode
    }

    override fun observeProjectTaskTimelineZoom(): Flow<TimelineZoom> = projectZoom

    override suspend fun saveProjectTaskTimelineZoom(zoom: TimelineZoom) {
        projectZoom.value = zoom
    }

    override fun observeProjectTaskTimelineAnchor(): Flow<Long?> = projectAnchor

    override suspend fun saveProjectTaskTimelineAnchor(anchorEpochDay: Long?) {
        projectAnchor.value = anchorEpochDay
    }
}
