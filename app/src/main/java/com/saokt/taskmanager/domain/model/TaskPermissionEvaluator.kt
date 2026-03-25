package com.saokt.taskmanager.domain.model

object TaskPermissionEvaluator {
    fun canMoveTask(
        task: Task,
        currentUserId: String?,
        project: Project?,
        members: List<ProjectMember>
    ): Boolean {
        if (currentUserId.isNullOrBlank()) {
            return false
        }
        if (project == null) {
            return task.createdBy == currentUserId || task.assignedTo == currentUserId
        }
        if (project.ownerId == currentUserId) {
            return true
        }
        val membership = members.firstOrNull { it.userId == currentUserId }
        return when (membership?.role) {
            ProjectRole.ADMIN -> true
            ProjectRole.MEMBER -> task.assignedTo == currentUserId || task.createdBy == currentUserId
            ProjectRole.OWNER -> true
            null -> false
        }
    }
}
