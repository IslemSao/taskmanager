package com.saokt.taskmanager.presentation.tasks.detail

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectMember
import com.saokt.taskmanager.domain.model.Subtask
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.task.AssignTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.CreateTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdRemoteUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdUseCase
import com.saokt.taskmanager.domain.usecase.task.UpdateTaskUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import com.saokt.taskmanager.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTaskByIdUseCase = mockk<GetTaskByIdUseCase>()
    private val createTaskUseCase = mockk<CreateTaskUseCase>()
    private val updateTaskUseCase = mockk<UpdateTaskUseCase>()
    private val assignTaskUseCase = mockk<AssignTaskUseCase>()
    private val getProjectsUseCase = mockk<GetProjectsUseCase>()
    private val getProjectByIdUseCase = mockk<GetProjectByIdUseCase>()
    private val getProjectMembersUseCase = mockk<GetProjectMembersUseCase>()
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val getTaskByIdRemoteUseCase = mockk<GetTaskByIdRemoteUseCase>()

    private lateinit var viewModel: TaskDetailViewModel

    @Before
    fun setUp() {
        every { getProjectsUseCase() } returns flowOf(emptyList())
        every { getCurrentUserUseCase() } returns flowOf(User(id = "u1", email = "u1@example.com"))
        every { getTaskByIdUseCase(any()) } returns flowOf(null)
        coEvery { getTaskByIdRemoteUseCase(any()) } returns Result.success(null)
        every { getProjectByIdUseCase(any()) } returns flowOf(null)
        coEvery { getProjectMembersUseCase(any()) } returns flowOf(emptyList())
        coEvery { createTaskUseCase(any()) } answers { Result.success(firstArg()) }
        coEvery { updateTaskUseCase(any()) } answers { Result.success(firstArg()) }
        coEvery { assignTaskUseCase(any(), any(), any()) } answers { Result.failure(IllegalStateException("unused")) }

        viewModel = createViewModel()
    }

    @Test
    fun `loadTask new initializes creation mode with initial project`() = runTest {
        viewModel.loadTask(taskId = "new", initialProjectId = "project-1")
        advanceUntilIdle()

        assertThat(viewModel.state.value.isNewTask).isTrue()
        assertThat(viewModel.state.value.task.projectId).isEqualTo("project-1")
        assertThat(viewModel.state.value.task.priority).isEqualTo(Priority.MEDIUM)
    }

    @Test
    fun `saveTask with blank title returns validation error`() = runTest {
        viewModel.loadTask("new")
        viewModel.updateTitle("   ")

        viewModel.saveTask()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo("Task title cannot be empty")
        coVerify(exactly = 0) { createTaskUseCase(any()) }
    }

    @Test
    fun `saveTask fails when user is not authenticated`() = runTest {
        every { getCurrentUserUseCase() } returns flowOf(null)
        viewModel = createViewModel()
        viewModel.loadTask("new")
        viewModel.updateTitle("Build tests")

        viewModel.saveTask()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo("User not authenticated")
        coVerify(exactly = 0) { createTaskUseCase(any()) }
    }

    @Test
    fun `saveTask success marks task saved and clears error`() = runTest {
        viewModel.loadTask("new")
        viewModel.updateTitle("Create task")
        viewModel.updateDescription("Task description")

        viewModel.saveTask()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isTaskSaved).isTrue()
        assertThat(viewModel.state.value.error).isNull()
        coVerify(exactly = 1) { createTaskUseCase(match { it.title == "Create task" }) }
    }

    @Test
    fun `saveTask failure uses fallback message when exception has no message`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Result.failure(IllegalStateException())
        viewModel.loadTask("new")
        viewModel.updateTitle("Create task")

        viewModel.saveTask()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isTaskSaved).isFalse()
        assertThat(viewModel.state.value.error).isEqualTo("Failed to save task")
    }

    @Test
    fun `duplicate save while loading is ignored`() = runTest {
        coEvery { createTaskUseCase(any()) } coAnswers {
            delay(100)
            Result.success(firstArg())
        }
        viewModel.loadTask("new")
        viewModel.updateTitle("No duplicate saves")

        viewModel.saveTask()
        viewModel.saveTask()
        advanceUntilIdle()

        coVerify(exactly = 1) { createTaskUseCase(any()) }
    }

    @Test
    fun `updateProject null clears project members and owner status`() = runTest {
        every { getProjectsUseCase() } returns flowOf(listOf(Project(id = "p1", title = "Project One")))
        viewModel = createViewModel()
        viewModel.loadTask("new", "p1")
        advanceUntilIdle()

        viewModel.updateProject(null)
        advanceUntilIdle()

        assertThat(viewModel.state.value.task.projectId).isNull()
        assertThat(viewModel.state.value.projectMembers).isEmpty()
        assertThat(viewModel.state.value.isProjectOwner).isFalse()
    }

    @Test
    fun `loadTask existing task uses local task when available`() = runTest {
        val localTask = Task(id = "t1", title = "Local task", projectId = "p1")
        every { getTaskByIdUseCase("t1") } returns flowOf(localTask)
        every { getProjectByIdUseCase("p1") } returns flowOf(Project(id = "p1", title = "Project", ownerId = "u1"))
        coEvery { getProjectMembersUseCase("p1") } returns flowOf(emptyList())
        viewModel = createViewModel()

        viewModel.loadTask("t1")
        advanceUntilIdle()

        assertThat(viewModel.state.value.task.id).isEqualTo("t1")
        assertThat(viewModel.state.value.task.title).isEqualTo("Local task")
    }

    @Test
    fun `loadTask falls back to remote when local task missing`() = runTest {
        val remoteTask = Task(id = "remote-1", title = "Fetched task")
        every { getTaskByIdUseCase("remote-1") } returns flowOf(null)
        coEvery { getTaskByIdRemoteUseCase("remote-1") } returns Result.success(remoteTask)
        viewModel = createViewModel()

        viewModel.loadTask("remote-1")
        advanceUntilIdle()

        assertThat(viewModel.state.value.task.id).isEqualTo("remote-1")
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `loadTask sets not found error when local and remote are missing`() = runTest {
        every { getTaskByIdUseCase("missing") } returns flowOf(null)
        coEvery { getTaskByIdRemoteUseCase("missing") } returns Result.failure(IllegalStateException("Not found"))
        viewModel = createViewModel()

        viewModel.loadTask("missing")
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo("Task not found")
    }

    @Test
    fun `loadTask with project context marks current user as owner and loads members`() = runTest {
        val member = ProjectMember(projectId = "p-owner", userId = "m1", email = "m1@example.com", displayName = "Member One")
        every { getProjectByIdUseCase("p-owner") } returns flowOf(Project(id = "p-owner", title = "Owned", ownerId = "u1"))
        coEvery { getProjectMembersUseCase("p-owner") } returns flowOf(listOf(member))
        viewModel = createViewModel()

        viewModel.loadTask("new", "p-owner")
        advanceUntilIdle()

        assertThat(viewModel.state.value.isProjectOwner).isTrue()
        assertThat(viewModel.state.value.projectMembers).hasSize(1)
        assertThat(viewModel.state.value.projectMembers.first().displayName).isEqualTo("Member One")
    }

    @Test
    fun `updatePriority due date and subtasks mutate task draft state`() = runTest {
        val dueDate = java.util.Date(1_700_000_000_000L)
        viewModel.loadTask("new")
        viewModel.updatePriority(Priority.HIGH)
        viewModel.updateDueDate(dueDate)
        viewModel.addSubtask("Write docs")
        val addedId = viewModel.state.value.task.subtasks.first().id
        viewModel.toggleSubtaskCompletion(addedId)
        viewModel.removeSubtask(addedId)
        advanceUntilIdle()

        assertThat(viewModel.state.value.task.priority).isEqualTo(Priority.HIGH)
        assertThat(viewModel.state.value.task.dueDate).isEqualTo(dueDate)
        assertThat(viewModel.state.value.task.subtasks).isEmpty()
    }

    @Test
    fun `saveTask passes project and assignee to create use case`() = runTest {
        viewModel.loadTask("new", "p1")
        viewModel.updateTitle("Assigned task")
        viewModel.updateAssignee("member-9")

        viewModel.saveTask()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            createTaskUseCase(match { it.projectId == "p1" && it.assignedTo == "member-9" && it.title == "Assigned task" })
        }
    }

    @Test
    fun `clearError resets existing error state`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Result.failure(IllegalStateException("boom"))
        viewModel.loadTask("new")
        viewModel.updateTitle("Will fail")
        viewModel.saveTask()
        advanceUntilIdle()
        assertThat(viewModel.state.value.error).isEqualTo("boom")

        viewModel.clearError()

        assertThat(viewModel.state.value.error).isNull()
    }

    private fun createViewModel(): TaskDetailViewModel {
        return TaskDetailViewModel(
            getTaskByIdUseCase = getTaskByIdUseCase,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            assignTaskUseCase = assignTaskUseCase,
            getProjectsUseCase = getProjectsUseCase,
            getProjectByIdUseCase = getProjectByIdUseCase,
            getProjectMembersUseCase = getProjectMembersUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            getTaskByIdRemoteUseCase = getTaskByIdRemoteUseCase
        )
    }
}
