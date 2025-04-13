// 1. First, update the Project domain model:
package com.example.taskmanager.domain.model

import java.util.Date
import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val color: Int? = null,
    val startDate: Date? = null,
    val dueDate: Date? = null,
    val isCompleted: Boolean = false,
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val ownerId: String = "", // Owner's user ID
    val members: List<ProjectMember> = emptyList() // Users who have access to this project
)

data class ProjectMember(
    val projectId: String,
    val userId: String,
    val email: String, // For display purposes
    val displayName: String, // For display purposes
    val role: ProjectRole = ProjectRole.MEMBER
)

enum class ProjectRole {
    OWNER, // Can do everything including deleting the project and managing members
    ADMIN, // Can edit project and manage tasks
    MEMBER // Can view project and only edit their assigned tasks
}
