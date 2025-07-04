package com.example.taskmanager.presentation.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object Dashboard : Screen("dashboard")
    object TaskList : Screen("task_list")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String = "new") = "task_detail/$taskId"
    }
    object ProjectList : Screen("project_list")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: String = "new") = "project_detail/$projectId"
    }
    object Calendar : Screen("calendar")
    object AddProject : Screen("add_project")
    object Invitations : Screen("invitations")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
}
