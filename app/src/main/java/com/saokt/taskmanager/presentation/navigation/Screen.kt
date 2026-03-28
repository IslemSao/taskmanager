package com.saokt.taskmanager.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    /** Bottom tab shell: [Dashboard], [TaskList], [Calendar], [ProjectList], [Profile]. */
    object MainTabs : Screen("main_tabs")
    object Dashboard : Screen("dashboard")
    object TaskList : Screen("task_list")
    object TaskDetail : Screen("task_detail/{taskId}?projectId={projectId}") {
        fun createRoute(taskId: String = "new", projectId: String? = null): String {
            val encodedTaskId = Uri.encode(taskId)
            val encodedProjectId = projectId?.let(Uri::encode).orEmpty()
            return "task_detail/$encodedTaskId?projectId=$encodedProjectId"
        }
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
            val encodedProjectId = Uri.encode(projectId)
            val encodedTaskId = Uri.encode(taskId.orEmpty())
            val encodedParticipants = Uri.encode(participantsCsv)
            val encodedCurrentUserId = Uri.encode(currentUserId)
            return "chat/$encodedProjectId?taskId=$encodedTaskId&participants=$encodedParticipants&currentUserId=$encodedCurrentUserId"
        }
    }
}
