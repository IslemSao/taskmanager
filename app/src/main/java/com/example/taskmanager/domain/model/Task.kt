package com.example.taskmanager.domain.model

import java.util.Date
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueDate: Date? = null,
    val completed: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val projectId: String? = null,
    val labels: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class SyncStatus {
    PENDING, SYNCED, SYNC_FAILED
}

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)
