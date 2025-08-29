package com.saokt.taskmanager.data.remote.dto

import java.util.Date

data class TaskDto(
    val id: String = "", // Default value, set ID later

    val title: String = "",
    val description: String = "",
    val dueDate: Date? = null,

    val completed: Boolean = false,

    val priority: String = "MEDIUM", // Default value
    val projectId: String? = null,
    val labels: List<String> = emptyList(),
    val subtasks: List<Map<String, Any>> = emptyList(),
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date(),
    val userId: String = "",
    val createdBy: String = "", // User who created the task
    val assignedTo: String? = null, // User assigned to the task
    val assignedAt: Date? = null, // When the task was assigned
    val assignedBy: String? = null // User who assigned the task
)