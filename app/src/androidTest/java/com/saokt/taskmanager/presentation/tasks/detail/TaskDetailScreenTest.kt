package com.saokt.taskmanager.presentation.tasks.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectInvitation
import com.saokt.taskmanager.domain.model.ProjectMember
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
import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.testing.runWithScreenshotOnFailure
import com.saokt.taskmanager.ui.theme.TaskmanagerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class TaskDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun taskCreation_showsValidationErrorWhenSavingBlankTitleFromToolbar() = composeRule.runWithScreenshotOnFailure("task_create_blank_title_error") {
        val fakeTaskRepository = FakeTaskRepository()
        val fakeUserRepository = FakeUserRepository()
        val fakeProjectRepository = FakeProjectRepository()
        val viewModel = createViewModel(fakeTaskRepository, fakeUserRepository, fakeProjectRepository)

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel,
                    taskId = "new"
                )
            }
        }

        composeRule.onNodeWithContentDescription("Save").performClick()
        composeRule.onNodeWithText("Task title cannot be empty").assertIsDisplayed()
    }

    @Test
    fun taskCreation_acceptsUnicodeTitleAndDescription() = composeRule.runWithScreenshotOnFailure("task_create_unicode_input") {
        val fakeTaskRepository = FakeTaskRepository()
        val fakeUserRepository = FakeUserRepository()
        val fakeProjectRepository = FakeProjectRepository()
        val viewModel = createViewModel(fakeTaskRepository, fakeUserRepository, fakeProjectRepository)

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel,
                    taskId = "new"
                )
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("إنهاء تقرير")
        composeRule.onNodeWithTag(TaskDetailTestTags.DESCRIPTION_FIELD).performTextInput("مراجعة نهائية قبل التسليم")
        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).assertTextContains("إنهاء تقرير")
        composeRule.onNodeWithTag(TaskDetailTestTags.DESCRIPTION_FIELD).assertTextContains("مراجعة نهائية قبل التسليم")
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).assertIsEnabled()
    }

    @Test
    fun taskCreation_loadingStateDisablesSaveAndShowsSavingText() = composeRule.runWithScreenshotOnFailure("task_create_loading_state") {
        val fakeTaskRepository = FakeTaskRepository(createDelayMs = 800)
        val fakeUserRepository = FakeUserRepository()
        val fakeProjectRepository = FakeProjectRepository()
        val viewModel = createViewModel(fakeTaskRepository, fakeUserRepository, fakeProjectRepository)

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel,
                    taskId = "new"
                )
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("Create loading test")
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).performClick()

        composeRule.onNodeWithText("Saving...").assertIsDisplayed()
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun taskCreation_projectScopedRoutePreselectsProject() = composeRule.runWithScreenshotOnFailure("task_create_project_scoped") {
        val fakeTaskRepository = FakeTaskRepository()
        val fakeUserRepository = FakeUserRepository()
        val fakeProjectRepository = FakeProjectRepository(
            projects = listOf(Project(id = "p1", title = "Roadmap Q2", ownerId = "u1"))
        )
        val viewModel = createViewModel(fakeTaskRepository, fakeUserRepository, fakeProjectRepository)

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel,
                    taskId = "new",
                    initialProjectId = "p1"
                )
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.PROJECT_DROPDOWN).assertTextContains("Roadmap Q2")
    }

    @Test
    fun taskCreation_saveDisabledUntilTitleProvided() = composeRule.runWithScreenshotOnFailure("task_create_save_disabled_until_title") {
        val fakeTaskRepository = FakeTaskRepository()
        val viewModel = createViewModel(fakeTaskRepository, FakeUserRepository(), FakeProjectRepository())

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(rememberNavController(), viewModel, taskId = "new")
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("Non empty title")
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).assertIsEnabled()
    }

    @Test
    fun taskCreation_prioritySelectionPersistsOnSave() = composeRule.runWithScreenshotOnFailure("task_create_priority_persisted") {
        val fakeTaskRepository = FakeTaskRepository()
        val viewModel = createViewModel(fakeTaskRepository, FakeUserRepository(), FakeProjectRepository())

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(rememberNavController(), viewModel, taskId = "new")
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("High priority item")
        composeRule.onNodeWithTag(TaskDetailTestTags.PRIORITY_HIGH).performClick()
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).performClick()
        composeRule.waitForIdle()

        assertEquals(com.saokt.taskmanager.domain.model.Priority.HIGH, fakeTaskRepository.lastCreatedTask?.priority)
    }

    @Test
    fun taskCreation_addSubtaskIncludesItInSavedTask() = composeRule.runWithScreenshotOnFailure("task_create_subtask_saved") {
        val fakeTaskRepository = FakeTaskRepository()
        val viewModel = createViewModel(fakeTaskRepository, FakeUserRepository(), FakeProjectRepository())

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(rememberNavController(), viewModel, taskId = "new")
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("Task with subtasks")
        composeRule.runOnIdle {
            viewModel.addSubtask("First subtask")
        }
        composeRule.onNodeWithText("First subtask").assertIsDisplayed()
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            fakeTaskRepository.lastCreatedTask != null
        }

        assertTrue(fakeTaskRepository.lastCreatedTask?.subtasks?.map { it.title }?.contains("First subtask") == true)
    }

    @Test
    fun taskCreation_failureShowsRepositoryMessage() = composeRule.runWithScreenshotOnFailure("task_create_failure_message") {
        val fakeTaskRepository = FakeTaskRepository(createFailure = IllegalStateException("Cannot save right now"))
        val viewModel = createViewModel(fakeTaskRepository, FakeUserRepository(), FakeProjectRepository())

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(rememberNavController(), viewModel, taskId = "new")
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("Will fail")
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).performClick()
        composeRule.onNodeWithText("Cannot save right now").assertIsDisplayed()
    }

    @Test
    fun taskCreation_singleSaveWhileLoadingCreatesOnlyOneTaskCall() = composeRule.runWithScreenshotOnFailure("task_create_single_call_while_loading") {
        val fakeTaskRepository = FakeTaskRepository(createDelayMs = 1000)
        val viewModel = createViewModel(fakeTaskRepository, FakeUserRepository(), FakeProjectRepository())

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(rememberNavController(), viewModel, taskId = "new")
            }
        }

        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("No duplicates")
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).performClick()
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).assertIsNotEnabled()
        composeRule.waitForIdle()

        assertEquals(1, fakeTaskRepository.createCallCount)
    }

    @Test
    fun taskCreation_ownerSeesAssignmentSectionForSelectedProject() = composeRule.runWithScreenshotOnFailure("task_create_owner_assignment_visible") {
        val fakeTaskRepository = FakeTaskRepository()
        val fakeProjectRepository = FakeProjectRepository(
            projects = listOf(Project(id = "p1", title = "Roadmap Q2", ownerId = "u1")),
            membersByProject = mapOf(
                "p1" to listOf(
                    ProjectMember(projectId = "p1", userId = "u1", email = "owner@example.com", displayName = "Owner"),
                    ProjectMember(projectId = "p1", userId = "m1", email = "m1@example.com", displayName = "Member One")
                )
            )
        )
        val viewModel = createViewModel(fakeTaskRepository, FakeUserRepository(), fakeProjectRepository)

        composeRule.setContent {
            TaskmanagerTheme {
                TaskDetailScreen(
                    navController = rememberNavController(),
                    viewModel = viewModel,
                    taskId = "new",
                    initialProjectId = "p1"
                )
            }
        }

        composeRule.onNodeWithText("Assign Task").assertIsDisplayed()
        composeRule.onNodeWithText("Assign to").assertIsDisplayed()
    }

    private fun createViewModel(
        taskRepository: TaskRepository,
        userRepository: UserRepository,
        projectRepository: ProjectRepository
    ): TaskDetailViewModel {
        return TaskDetailViewModel(
            getTaskByIdUseCase = GetTaskByIdUseCase(taskRepository),
            createTaskUseCase = CreateTaskUseCase(taskRepository, userRepository),
            updateTaskUseCase = UpdateTaskUseCase(taskRepository, userRepository),
            assignTaskUseCase = AssignTaskUseCase(taskRepository, userRepository),
            getProjectsUseCase = GetProjectsUseCase(projectRepository),
            getProjectByIdUseCase = GetProjectByIdUseCase(projectRepository),
            getProjectMembersUseCase = GetProjectMembersUseCase(projectRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository),
            getTaskByIdRemoteUseCase = GetTaskByIdRemoteUseCase(taskRepository)
        )
    }
}

private class FakeTaskRepository(
    private val createDelayMs: Long = 0L,
    private val createFailure: Throwable? = null
) : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    var createCallCount: Int = 0
        private set
    var lastCreatedTask: Task? = null
        private set

    override fun getAllTasks(): Flow<List<Task>> = tasks

    override fun getTaskById(taskId: String): Flow<Task?> = flowOf(tasks.value.find { it.id == taskId })

    override fun getTasksByProject(projectId: String): Flow<List<Task>> =
        flowOf(tasks.value.filter { it.projectId == projectId })

    override suspend fun getTasksByProjectFromFirebase(projectId: String): Result<List<Task>> =
        Result.success(tasks.value.filter { it.projectId == projectId })

    override suspend fun fetchTaskByIdRemote(taskId: String): Result<Task?> =
        Result.success(tasks.value.find { it.id == taskId })

    override suspend fun createTask(task: Task): Result<Task> {
        createCallCount += 1
        if (createDelayMs > 0) delay(createDelayMs)
        createFailure?.let { return Result.failure(it) }
        lastCreatedTask = task
        tasks.value = tasks.value + task
        return Result.success(task)
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
        return Result.success(task)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        tasks.value = tasks.value.filterNot { it.id == taskId }
        return Result.success(Unit)
    }

    override suspend fun completeTask(taskId: String): Result<Task> = Result.failure(UnsupportedOperationException())
    override suspend fun syncPendingTasks(): Result<Int> = Result.success(0)
    override suspend fun toggleTaskCompletion(task: Task): Result<Task> = Result.failure(UnsupportedOperationException())
}

private class FakeProjectRepository(
    projects: List<Project> = emptyList(),
    private val membersByProject: Map<String, List<ProjectMember>> = emptyMap()
) : ProjectRepository {
    private val projectFlow = MutableStateFlow(projects)

    override fun getAllProjects(): Flow<List<Project>> = projectFlow
    override fun getProjectById(projectId: String): Flow<Project?> = flowOf(projectFlow.value.find { it.id == projectId })
    override suspend fun createProject(project: Project): Result<Project> = Result.success(project)
    override suspend fun updateProject(project: Project): Result<Project> = Result.success(project)
    override suspend fun deleteProject(projectId: String): Result<Unit> = Result.success(Unit)
    override suspend fun completeProject(projectId: String): Result<Project> = Result.failure(UnsupportedOperationException())
    override suspend fun syncPendingProjects(): Result<Int> = Result.success(0)
    override suspend fun inviteUserToProject(projectId: String, projectTitle: String, userEmail: String): Result<ProjectInvitation> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getProjectInvitations(): Flow<List<ProjectInvitation>> = flowOf(emptyList())
    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun getProjectMembers(projectId: String): Flow<List<ProjectMember>> = flowOf(membersByProject[projectId].orEmpty())
    override suspend fun removeMemberFromProject(projectId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getProjectMembers(): Flow<List<ProjectMember>> = flowOf(emptyList())
    override suspend fun inviteProjectMember(projectId: String, projectTitle: String, userEmail: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit> = Result.success(Unit)
}

private class FakeUserRepository(
    private val currentUser: User? = User(id = "u1", email = "u1@example.com")
) : UserRepository {
    override fun getCurrentUser(): Flow<User?> = flowOf(currentUser)
    override fun isUserAuthenticated(): Boolean = currentUser != null
    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(UnsupportedOperationException())
    override suspend fun signInWithGoogle(idToken: String): Result<User> = Result.failure(UnsupportedOperationException())
    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> =
        Result.failure(UnsupportedOperationException())
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendEmailVerification(): Result<Unit> = Result.success(Unit)
    override suspend fun isCurrentUserEmailVerified(forceRefresh: Boolean): Result<Boolean> = Result.success(true)
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    override fun listenToRemoteProjects(): Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>> =
        flowOf(Result.success(emptyList<ProjectDto>() to emptyList()))
    override fun listenToRemoteTasks(): Flow<Result<List<TaskDto>>> = flowOf(Result.success(emptyList()))
    override fun listenToRemoteInvitations(): Flow<Result<List<ProjectInvitationDto>>> = flowOf(Result.success(emptyList()))
    override suspend fun syncRemoteProjectsToLocal(projectDtos: List<ProjectDto>) = Unit
    override suspend fun syncRemoteTasksToLocal(taskDtos: List<TaskDto>) = Unit
    override suspend fun syncRemoteInvitationsToLocal(projectInvitationDtos: List<ProjectInvitationDto>) = Unit
    override suspend fun syncRemoteMembersToLocal(projectMemberDtos: List<ProjectMemberDto>) = Unit
    override suspend fun updateUser(user: User): Result<Unit> = Result.success(Unit)
}
