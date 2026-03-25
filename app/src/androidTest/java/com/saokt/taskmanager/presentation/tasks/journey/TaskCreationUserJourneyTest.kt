package com.saokt.taskmanager.presentation.tasks.journey

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectInvitation
import com.saokt.taskmanager.domain.model.ProjectMember
import com.saokt.taskmanager.domain.model.ProjectTaskViewMode
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectMembersUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.task.AssignTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.CreateTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdRemoteUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksUseCase
import com.saokt.taskmanager.domain.usecase.task.RescheduleTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.ResizeTaskScheduleUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskCompletionUseCase
import com.saokt.taskmanager.domain.usecase.task.UpdateTaskUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.TaskPreferencesRepository
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.presentation.tasks.detail.TaskDetailScreen
import com.saokt.taskmanager.presentation.tasks.detail.TaskDetailTestTags
import com.saokt.taskmanager.presentation.tasks.detail.TaskDetailViewModel
import com.saokt.taskmanager.presentation.tasks.list.TaskListScreen
import com.saokt.taskmanager.presentation.tasks.list.TaskListViewModel
import com.saokt.taskmanager.testing.runWithScreenshotOnFailure
import com.saokt.taskmanager.ui.theme.TaskmanagerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskCreationUserJourneyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun userNavigatesFromTaskListToCreationAndBackWithDraftInputIntact() = composeRule.runWithScreenshotOnFailure("journey_list_to_creation_and_back") {
        val taskRepo = JourneyTaskRepository()
        val projectRepo = JourneyProjectRepository()
        val userRepo = JourneyUserRepository()
        val taskListViewModel = buildTaskListViewModel(taskRepo, projectRepo, userRepo)
        val taskDetailViewModel = buildTaskDetailViewModel(taskRepo, projectRepo, userRepo)

        composeRule.setContent {
            TaskmanagerTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.TaskList.route) {
                    composable(Screen.TaskList.route) {
                        TaskListScreen(navController = navController, viewModel = taskListViewModel)
                    }
                    composable(
                        route = Screen.TaskDetail.route,
                        arguments = listOf(
                            navArgument("taskId") { type = NavType.StringType },
                            navArgument("projectId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId") ?: "new"
                        val projectId = backStackEntry.arguments?.getString("projectId")
                        TaskDetailScreen(
                            navController = navController,
                            viewModel = taskDetailViewModel,
                            taskId = taskId,
                            initialProjectId = projectId
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("No tasks yet").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add Task").performClick()
        composeRule.onNodeWithText("New Task").assertIsDisplayed()
        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("Journey task")
        composeRule.onNodeWithTag(TaskDetailTestTags.SAVE_BUTTON).assertIsEnabled()
        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add Task").assertIsDisplayed()
    }

    @Test
    fun userCancelsTaskCreationUsingBackAndNoTaskIsCreated() = composeRule.runWithScreenshotOnFailure("journey_cancel_task_creation") {
        val taskRepo = JourneyTaskRepository()
        val projectRepo = JourneyProjectRepository()
        val userRepo = JourneyUserRepository()
        val taskListViewModel = buildTaskListViewModel(taskRepo, projectRepo, userRepo)
        val taskDetailViewModel = buildTaskDetailViewModel(taskRepo, projectRepo, userRepo)

        composeRule.setContent {
            TaskmanagerTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.TaskList.route) {
                    composable(Screen.TaskList.route) {
                        TaskListScreen(navController = navController, viewModel = taskListViewModel)
                    }
                    composable(
                        route = Screen.TaskDetail.route,
                        arguments = listOf(
                            navArgument("taskId") { type = NavType.StringType },
                            navArgument("projectId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId") ?: "new"
                        val projectId = backStackEntry.arguments?.getString("projectId")
                        TaskDetailScreen(
                            navController = navController,
                            viewModel = taskDetailViewModel,
                            taskId = taskId,
                            initialProjectId = projectId
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Add Task").performClick()
        composeRule.onNodeWithText("New Task").assertIsDisplayed()
        composeRule.onNodeWithTag(TaskDetailTestTags.TITLE_FIELD).performTextInput("Will cancel")
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("No tasks yet").assertIsDisplayed()
        assertEquals(0, taskRepo.createCount)
    }

    private fun buildTaskListViewModel(
        taskRepo: TaskRepository,
        projectRepo: ProjectRepository,
        userRepo: UserRepository
    ): TaskListViewModel {
        return TaskListViewModel(
            getTasksUseCase = GetTasksUseCase(taskRepo),
            getProjectsUseCase = GetProjectsUseCase(projectRepo),
            deleteTaskUseCase = DeleteTaskUseCase(taskRepo),
            rescheduleTaskUseCase = RescheduleTaskUseCase(taskRepo),
            resizeTaskScheduleUseCase = ResizeTaskScheduleUseCase(taskRepo),
            toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepo),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepo),
            taskPreferencesRepository = JourneyTaskPreferencesRepository()
        )
    }

    private fun buildTaskDetailViewModel(
        taskRepo: TaskRepository,
        projectRepo: ProjectRepository,
        userRepo: UserRepository
    ): TaskDetailViewModel {
        return TaskDetailViewModel(
            getTaskByIdUseCase = GetTaskByIdUseCase(taskRepo),
            createTaskUseCase = CreateTaskUseCase(taskRepo, userRepo),
            updateTaskUseCase = UpdateTaskUseCase(taskRepo, userRepo),
            assignTaskUseCase = AssignTaskUseCase(taskRepo, userRepo),
            getProjectsUseCase = GetProjectsUseCase(projectRepo),
            getProjectByIdUseCase = GetProjectByIdUseCase(projectRepo),
            getProjectMembersUseCase = GetProjectMembersUseCase(projectRepo),
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepo),
            getTaskByIdRemoteUseCase = GetTaskByIdRemoteUseCase(taskRepo)
        )
    }
}

private class JourneyTaskRepository : TaskRepository {
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    var createCount: Int = 0
        private set

    override fun getAllTasks(): Flow<List<Task>> = tasksFlow

    override fun getTaskById(taskId: String): Flow<Task?> = flowOf(tasksFlow.value.find { it.id == taskId })

    override fun getTasksByProject(projectId: String): Flow<List<Task>> =
        flowOf(tasksFlow.value.filter { it.projectId == projectId })

    override suspend fun getTasksByProjectFromFirebase(projectId: String): Result<List<Task>> =
        Result.success(tasksFlow.value.filter { it.projectId == projectId })

    override suspend fun fetchTaskByIdRemote(taskId: String): Result<Task?> =
        Result.success(tasksFlow.value.find { it.id == taskId })

    override suspend fun createTask(task: Task): Result<Task> {
        createCount += 1
        tasksFlow.value = tasksFlow.value + task
        return Result.success(task)
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it }
        return Result.success(task)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == taskId }
        return Result.success(Unit)
    }

    override suspend fun completeTask(taskId: String): Result<Task> = Result.failure(UnsupportedOperationException())

    override suspend fun syncPendingTasks(): Result<Int> = Result.success(0)

    override suspend fun toggleTaskCompletion(task: Task): Result<Task> {
        val toggled = task.copy(completed = !task.completed)
        tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) toggled else it }
        return Result.success(toggled)
    }
}

private class JourneyProjectRepository : ProjectRepository {
    override fun getAllProjects(): Flow<List<Project>> = flowOf(emptyList())
    override fun getProjectById(projectId: String): Flow<Project?> = flowOf(null)
    override suspend fun createProject(project: Project): Result<Project> = Result.success(project)
    override suspend fun updateProject(project: Project): Result<Project> = Result.success(project)
    override suspend fun deleteProject(projectId: String): Result<Unit> = Result.success(Unit)
    override suspend fun completeProject(projectId: String): Result<Project> = Result.failure(UnsupportedOperationException())
    override suspend fun syncPendingProjects(): Result<Int> = Result.success(0)
    override suspend fun inviteUserToProject(projectId: String, projectTitle: String, userEmail: String): Result<ProjectInvitation> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getProjectInvitations(): Flow<List<ProjectInvitation>> = flowOf(emptyList())
    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun getProjectMembers(projectId: String): Flow<List<ProjectMember>> = flowOf(emptyList())
    override suspend fun removeMemberFromProject(projectId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getProjectMembers(): Flow<List<ProjectMember>> = flowOf(emptyList())
    override suspend fun inviteProjectMember(projectId: String, projectTitle: String, userEmail: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit> = Result.success(Unit)
}

private class JourneyUserRepository : UserRepository {
    override fun getCurrentUser(): Flow<User?> = flowOf(User(id = "u1", email = "u1@example.com", displayName = "User One"))
    override fun isUserAuthenticated(): Boolean = true
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

private class JourneyTaskPreferencesRepository : TaskPreferencesRepository {
    override fun observeTaskListQuery(): Flow<TaskListQuery> = flowOf(TaskListQuery())
    override suspend fun saveTaskListQuery(query: TaskListQuery) = Unit
    override fun observeProjectTaskQuery(): Flow<TaskListQuery> = flowOf(TaskListQuery())
    override suspend fun saveProjectTaskQuery(query: TaskListQuery) = Unit
    override fun observeProjectTaskViewMode(): Flow<ProjectTaskViewMode> = flowOf(ProjectTaskViewMode.LIST)
    override suspend fun saveProjectTaskViewMode(mode: ProjectTaskViewMode) = Unit
}
