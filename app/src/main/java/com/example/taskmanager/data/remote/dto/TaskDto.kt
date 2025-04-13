package com.example.taskmanager.data.remote.dto

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
    val userId: String = ""
)