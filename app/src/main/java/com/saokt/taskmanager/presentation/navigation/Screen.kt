package com.saokt.taskmanager.presentation.navigation

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
    object ProjectTasks : Screen("project_tasks/{projectId}") {
        fun createRoute(projectId: String) = "project_tasks/$projectId"
    }
    object ProjectMembers : Screen("project_members/{projectId}") {
        fun createRoute(projectId: String) = "project_members/$projectId"
    }
    object Calendar : Screen("calendar")
    object AddProject : Screen("add_project")
    object Invitations : Screen("invitations")
    object Notifications : Screen("notifications")
    object NotificationSettings : Screen("notification_settings")
    object Profile : Screen("profile")
    object Chat : Screen("chat/{projectId}?taskId={taskId}&participants={participants}&currentUserId={currentUserId}") {
        fun createRoute(projectId: String, taskId: String?, participantsCsv: String, currentUserId: String): String {
            val t = taskId ?: ""
            return "chat/$projectId?taskId=$t&participants=$participantsCsv&currentUserId=$currentUserId"
        }
    }
}
