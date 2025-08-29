package com.saokt.taskmanager.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

// Enhanced Notification Models

data class NotificationPreference(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val enabled: Boolean = true,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val trigger: NotificationTrigger = NotificationTrigger.TIME_BASED,
    val scheduleConfig: NotificationScheduleConfig = NotificationScheduleConfig(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val ledEnabled: Boolean = false,
    val ledColor: String = "#FF0000", // Default red
    val customMessage: String? = null,
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date()
)

data class NotificationScheduleConfig(
    val intervalHours: Int = 2,  // For periodic notifications
    val customTimes: List<LocalTime> = emptyList(),  // Specific times of day
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),  // Days to trigger
    val quietHoursStart: LocalTime? = null,  // Quiet hours start time
    val quietHoursEnd: LocalTime? = null,    // Quiet hours end time
    val dueDateThresholdHours: Int = 24,     // Hours before due date to trigger
    val priorityThreshold: Priority = Priority.MEDIUM,  // Minimum priority to trigger
    val smartSchedulingEnabled: Boolean = false,  // Use AI for optimal timing
    val locationBasedEnabled: Boolean = false,   // Location-based triggers
    val batteryOptimizationEnabled: Boolean = true  // Respect battery optimization
)

data class NotificationHistory(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val projectId: String? = null,
    val type: NotificationType,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val scheduledAt: Date,
    val shownAt: Date? = null,
    val clicked: Boolean = false,
    val dismissed: Boolean = false,
    val actionTaken: NotificationAction? = null
)

enum class NotificationAction {
    MARK_COMPLETE,
    SNOOZE,
    VIEW_DETAILS,
    DISMISS,
    OPEN_APP
}

// Smart Notification Rules
data class SmartNotificationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val enabled: Boolean = true,
    val triggerConditions: List<NotificationTriggerCondition>,
    val actions: List<NotificationActionType>,
    val priority: NotificationPriority = NotificationPriority.MEDIUM
)

data class NotificationTriggerCondition(
    val type: NotificationTriggerConditionType,
    val operator: ConditionOperator,
    val value: String,  // Flexible value (hours, priority, etc.)
    val additionalData: Map<String, Any> = emptyMap()
)

enum class NotificationTriggerConditionType {
    DUE_DATE_WITHIN_HOURS,
    TASK_PRIORITY_ABOVE,
    TASK_OVERDUE_BY_HOURS,
    USER_INACTIVITY_HOURS,
    TASK_COUNT_ABOVE,
    PROJECT_DEADLINE_WITHIN_DAYS,
    TIME_OF_DAY,
    DAY_OF_WEEK,
    USER_LOCATION
}

enum class ConditionOperator {
    EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    NOT_CONTAINS,
    IN_RANGE
}

enum class NotificationActionType {
    SHOW_NOTIFICATION,
    SCHEDULE_REMINDER,
    UPDATE_TASK_STATUS,
    SEND_EMAIL,
    TRIGGER_WORKFLOW
}

// ========== NOTIFICATION CONTENT DATA CLASSES ==========

data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val tasksCompletedToday: Int,
    val lastCompletionDate: Date
)

data class WeeklyStats(
    val tasksCompleted: Int,
    val tasksCreated: Int,
    val averageCompletionTime: Double, // hours
    val mostProductiveDay: String,
    val totalFocusTime: Double // hours
)

data class DayStats(
    val tasksCompleted: Int,
    val tasksRemaining: Int,
    val totalFocusTime: Double, // hours
    val productivity: Int // percentage
)

// User Notification Preferences Profile
data class NotificationProfile(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val isActive: Boolean = true,
    val preferences: List<NotificationPreference>,
    val globalSettings: GlobalNotificationSettings = GlobalNotificationSettings(),
    val createdAt: Date = Date(),
    val modifiedAt: Date = Date()
)

data class GlobalNotificationSettings(
    val masterNotificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(8, 0),
    val weekendQuietHoursEnabled: Boolean = true,
    val batteryOptimizationEnabled: Boolean = true,
    val locationBasedEnabled: Boolean = false,
    val smartSchedulingEnabled: Boolean = true,
    val maxNotificationsPerDay: Int = 50,
    val notificationRetentionDays: Int = 30,
    val allowNotificationGrouping: Boolean = true,
    val showNotificationBadge: Boolean = true,
    val playNotificationSound: Boolean = true,
    val useCustomNotificationSound: Boolean = false,
    val customSoundUri: String? = null,
    val vibrationPattern: List<Long> = listOf(0, 250, 250, 250), // Default pattern
    val ledEnabled: Boolean = true,
    val ledColor: String = "#FF0000",
    val showOnLockScreen: Boolean = true,
    val allowFullScreenNotifications: Boolean = false,
    val allowNotificationChannels: Boolean = true
)
