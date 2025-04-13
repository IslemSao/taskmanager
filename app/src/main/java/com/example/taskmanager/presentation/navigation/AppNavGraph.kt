package com.example.taskmanager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskmanager.presentation.authentication.signin.SignInScreen
import com.example.taskmanager.presentation.authentication.signin.SignInViewModel
import com.example.taskmanager.presentation.authentication.signup.SignUpScreen
import com.example.taskmanager.presentation.authentication.signup.SignUpViewModel
import com.example.taskmanager.presentation.dashboard.DashboardScreen
import com.example.taskmanager.presentation.dashboard.DashboardViewModel
import com.example.taskmanager.presentation.invitation.InvitationsScreen
import com.example.taskmanager.presentation.invitation.InvitationsViewModel
import com.example.taskmanager.presentation.notification.NotificationsScreen
import com.example.taskmanager.presentation.notification.NotificationsViewModel
import com.example.taskmanager.presentation.project.detail.ProjectDetailScreen
import com.example.taskmanager.presentation.project.detail.ProjectDetailViewModel
import com.example.taskmanager.presentation.project.list.ProjectListScreen
import com.example.taskmanager.presentation.project.list.ProjectListViewModel
import com.example.taskmanager.presentation.tasks.detail.TaskDetailScreen
import com.example.taskmanager.presentation.tasks.detail.TaskDetailViewModel
import com.example.taskmanager.presentation.tasks.list.TaskListScreen
import com.example.taskmanager.presentation.tasks.list.TaskListViewModel

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
            val singInViewModel: SignInViewModel = hiltViewModel()
            DashboardScreen(
                navController = navController,
                viewModel = viewModel,
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

        composable(route = Screen.Notifications.route) {
            val viewModel: InvitationsViewModel = hiltViewModel()
            InvitationsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}
