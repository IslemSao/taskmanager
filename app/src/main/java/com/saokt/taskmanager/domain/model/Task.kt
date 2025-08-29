package com.saokt.taskmanager.domain.model

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
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val userId: String = "", // User who owns the task (from Firebase)
    val createdBy: String = "", // User who created the task
    val assignedTo: String? = null, // User assigned to the task
    val assignedAt: Date? = null, // When the task was assigned
    val assignedBy: String? = null // User who assigned the task
)

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class SyncStatus {
    PENDING, SYNCED, SYNC_FAILED
}

// Enhanced Notification Types
enum class NotificationType {
    TASK_REMINDER,           // General task reminders
    DUE_SOON_ALERT,          // Tasks due within 24 hours
    OVERDUE_ALERT,           // Tasks that are overdue
    HIGH_PRIORITY_REMINDER,  // High priority task reminders
    PROJECT_UPDATE,          // Project-related notifications
    ASSIGNMENT_UPDATE,       // Task assignment notifications
    COMPLETION_CELEBRATION,  // Task completion celebrations
    STREAK_REMINDER,         // Daily streak maintenance
    DEADLINE_WARNING,        // Tasks due within custom time
    WEEKLY_SUMMARY,          // Weekly task summary
    MORNING_BRIEFING,        // Daily morning task overview
    EVENING_REVIEW           // Daily evening task review
}

// Notification Priority Levels
enum class NotificationPriority {
    LOW, MEDIUM, HIGH, URGENT
}

// Time-based Notification Triggers
enum class NotificationTrigger {
    TIME_BASED,      // Scheduled at specific times
    EVENT_BASED,     // Triggered by events (task creation, updates, etc.)
    LOCATION_BASED,  // Location-based triggers
    SMART_BASED      // AI/smart scheduling based on user behavior
}

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)
