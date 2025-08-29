package com.saokt.taskmanager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saokt.taskmanager.presentation.authentication.signin.SignInScreen
import com.saokt.taskmanager.presentation.authentication.signin.SignInViewModel
import com.saokt.taskmanager.presentation.authentication.signup.SignUpScreen
import com.saokt.taskmanager.presentation.authentication.signup.SignUpViewModel
import com.saokt.taskmanager.presentation.dashboard.DashboardScreen
import com.saokt.taskmanager.presentation.dashboard.DashboardViewModel
import com.saokt.taskmanager.presentation.invitation.InvitationsScreen
import com.saokt.taskmanager.presentation.invitation.InvitationsViewModel
import com.saokt.taskmanager.presentation.notification.NotificationsScreen
import com.saokt.taskmanager.presentation.notification.NotificationsViewModel
import com.saokt.taskmanager.presentation.notification.NotificationSettingsScreen
import com.saokt.taskmanager.presentation.notification.NotificationSettingsViewModel
import com.saokt.taskmanager.presentation.profile.ProfileScreen
import com.saokt.taskmanager.presentation.profile.ProfileViewModel
import com.saokt.taskmanager.presentation.project.detail.ProjectDetailScreen
import com.saokt.taskmanager.presentation.project.detail.ProjectDetailViewModel
import com.saokt.taskmanager.presentation.project.list.ProjectListScreen
import com.saokt.taskmanager.presentation.project.list.ProjectListViewModel
import com.saokt.taskmanager.presentation.project.members.ProjectMembersScreen
import com.saokt.taskmanager.presentation.project.members.ProjectMembersViewModel
import com.saokt.taskmanager.presentation.project.tasks.ProjectTasksScreen
import com.saokt.taskmanager.presentation.project.tasks.ProjectTasksViewModel
import com.saokt.taskmanager.presentation.tasks.detail.TaskDetailScreen
import com.saokt.taskmanager.presentation.tasks.detail.TaskDetailViewModel
import com.saokt.taskmanager.presentation.tasks.list.TaskListScreen
import com.saokt.taskmanager.presentation.tasks.list.TaskListViewModel
import com.saokt.taskmanager.presentation.calendar.CalendarScreen
import com.saokt.taskmanager.presentation.calendar.CalendarViewModel

@Composable
fun AppNavGraph(startDestination: String = Screen.SignIn.route) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SignIn.route) {
            val viewModel: SignInViewModel = hiltViewModel()
            SignInScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.SignUp.route) {
            val viewModel: SignUpViewModel = hiltViewModel()
            SignUpScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.TaskList.route) {
            val viewModel: TaskListViewModel = hiltViewModel()
            TaskListScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: "new"
            val viewModel: TaskDetailViewModel = hiltViewModel()
            TaskDetailScreen(
                navController = navController,
                viewModel = viewModel,
                taskId = taskId
            )
        }

        // In your NavGraph.kt, add these composables:
        composable(
            route = Screen.ProjectList.route
        ) {
            val viewModel = hiltViewModel<ProjectListViewModel>()
            ProjectListScreen(navController = navController, viewModel = viewModel)
        }

        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId")
            val viewModel = hiltViewModel<ProjectDetailViewModel>()
            ProjectDetailScreen(
                navController = navController,
                viewModel = viewModel,
                projectId = projectId
            )
        }
        // In your NavGraph:

        composable(
            route = Screen.AddProject.route
        ) {
            val viewModel = hiltViewModel<ProjectDetailViewModel>()
            ProjectDetailScreen(
                navController = navController,
                viewModel = viewModel,
                projectId = null
            )
        }

        composable(
            route = Screen.ProjectMembers.route,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId") ?: ""
            val viewModel = hiltViewModel<ProjectMembersViewModel>()
            ProjectMembersScreen(
                navController = navController,
                viewModel = viewModel,
                projectId = projectId
            )
        }

        composable(
            route = Screen.ProjectTasks.route,
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId") ?: ""
            val viewModel = hiltViewModel<ProjectTasksViewModel>()
            ProjectTasksScreen(
                navController = navController,
                viewModel = viewModel,
                projectId = projectId
            )
        }

        composable(route = Screen.Notifications.route) {
            val viewModel: NotificationsViewModel = hiltViewModel()
            NotificationsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(route = Screen.NotificationSettings.route) {
            val viewModel: NotificationSettingsViewModel = hiltViewModel()
            NotificationSettingsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(route = Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(route = Screen.Calendar.route) {
            val viewModel: CalendarViewModel = hiltViewModel()
            CalendarScreen(
                navController = navController,
                viewModel = viewModel,
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }
    }
}
