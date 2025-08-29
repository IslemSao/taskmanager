package com.saokt.taskmanager.notification

import android.content.Context
import com.saokt.taskmanager.domain.model.*
import com.saokt.taskmanager.domain.repository.TaskRepository
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class NotificationTypeHandler @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsManager: NotificationSettingsManager,
    private val notificationManager: TaskNotificationManager
) {

    /**
     * Handle notification for a specific type
     */
    suspend fun handleNotificationType(
        type: NotificationType,
        task: Task? = null,
        project: Project? = null,
        customData: Map<String, Any> = emptyMap()
    ) {
        // Check if notification type is enabled
        if (!settingsManager.isNotificationTypeEnabled(type)) {
            return
        }

        // Check if we're in quiet hours
        if (settingsManager.isInQuietHours()) {
            val globalSettings = settingsManager.getGlobalSettings()
            if (!globalSettings.quietHoursEnabled) {
                return
            }
        }

        when (type) {
            NotificationType.TASK_REMINDER -> handleTaskReminder()
            NotificationType.DUE_SOON_ALERT -> handleDueSoonAlert()
            NotificationType.OVERDUE_ALERT -> handleOverdueAlert()
            NotificationType.HIGH_PRIORITY_REMINDER -> handleHighPriorityReminder()
            NotificationType.PROJECT_UPDATE -> handleProjectUpdate(project)
            NotificationType.ASSIGNMENT_UPDATE -> handleAssignmentUpdate(task)
            NotificationType.COMPLETION_CELEBRATION -> handleCompletionCelebration(task)
            NotificationType.STREAK_REMINDER -> handleStreakReminder()
            NotificationType.DEADLINE_WARNING -> handleDeadlineWarning()
            NotificationType.WEEKLY_SUMMARY -> handleWeeklySummary()
            NotificationType.MORNING_BRIEFING -> handleMorningBriefing()
            NotificationType.EVENING_REVIEW -> handleEveningReview()
        }
    }

    private suspend fun handleTaskReminder() {
        val tasks = getPendingTasks()
        if (tasks.isNotEmpty()) {
            val preference = settingsManager.getNotificationPreference(NotificationType.TASK_REMINDER)
            notificationManager.showEnhancedTaskReminderNotification(tasks, preference)
        }
    }

    private suspend fun handleDueSoonAlert() {
        val tasks = getTasksDueSoon()
        tasks.forEach { task ->
            val preference = settingsManager.getNotificationPreference(NotificationType.DUE_SOON_ALERT)
            notificationManager.showEnhancedDueSoonNotification(task, preference)
        }
    }

    private suspend fun handleOverdueAlert() {
        val tasks = getOverdueTasks()
        tasks.forEach { task ->
            val preference = settingsManager.getNotificationPreference(NotificationType.OVERDUE_ALERT)
            notificationManager.showEnhancedOverdueNotification(task, preference)
        }
    }

    private suspend fun handleHighPriorityReminder() {
        val tasks = getHighPriorityTasks()
        if (tasks.isNotEmpty()) {
            val preference = settingsManager.getNotificationPreference(NotificationType.HIGH_PRIORITY_REMINDER)
            notificationManager.showEnhancedHighPriorityNotification(tasks, preference)
        }
    }

    private suspend fun handleProjectUpdate(project: Project?) {
        project?.let {
            val preference = settingsManager.getNotificationPreference(NotificationType.PROJECT_UPDATE)
            notificationManager.showEnhancedProjectUpdateNotification(it, preference)
        }
    }

    private suspend fun handleAssignmentUpdate(task: Task?) {
        task?.let {
            val preference = settingsManager.getNotificationPreference(NotificationType.ASSIGNMENT_UPDATE)
            notificationManager.showEnhancedAssignmentNotification(it, preference)
        }
    }

    private suspend fun handleCompletionCelebration(task: Task?) {
        task?.let {
            val preference = settingsManager.getNotificationPreference(NotificationType.COMPLETION_CELEBRATION)
            notificationManager.showEnhancedCelebrationNotification(it, preference)
        }
    }

    private suspend fun handleStreakReminder() {
        val streakData = getStreakData()
        if (streakData.currentStreak > 0) {
            val preference = settingsManager.getNotificationPreference(NotificationType.STREAK_REMINDER)
            notificationManager.showEnhancedStreakNotification(streakData, preference)
        }
    }

    private suspend fun handleDeadlineWarning() {
        val tasks = getTasksNearDeadline()
        tasks.forEach { task ->
            val preference = settingsManager.getNotificationPreference(NotificationType.DEADLINE_WARNING)
            notificationManager.showEnhancedDeadlineWarningNotification(task, preference)
        }
    }

    private suspend fun handleWeeklySummary() {
        val weeklyStats = getWeeklyStats()
        val preference = settingsManager.getNotificationPreference(NotificationType.WEEKLY_SUMMARY)
        notificationManager.showEnhancedWeeklySummaryNotification(weeklyStats, preference)
    }

    private suspend fun handleMorningBriefing() {
        val todayTasks = getTodayTasks()
        val preference = settingsManager.getNotificationPreference(NotificationType.MORNING_BRIEFING)
        notificationManager.showEnhancedMorningBriefingNotification(todayTasks, preference)
    }

    private suspend fun handleEveningReview() {
        val dayStats = getDayStats()
        val preference = settingsManager.getNotificationPreference(NotificationType.EVENING_REVIEW)
        notificationManager.showEnhancedEveningReviewNotification(dayStats, preference)
    }

    // ========== DATA RETRIEVAL METHODS ==========

    private suspend fun getPendingTasks(): List<Task> {
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        return allTasks.filter { task -> !task.completed }
    }

    private suspend fun getTasksDueSoon(hours: Int = 24): List<Task> {
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        val currentDate = Date()
        val nextHours = Date(currentDate.time + TimeUnit.HOURS.toMillis(hours.toLong()))

        return allTasks.filter { task ->
            !task.completed &&
            task.dueDate != null &&
            task.dueDate.after(currentDate) &&
            task.dueDate.before(nextHours)
        }
    }

    private suspend fun getOverdueTasks(): List<Task> {
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        val currentDate = Date()

        return allTasks.filter { task ->
            !task.completed &&
            task.dueDate != null &&
            task.dueDate.before(currentDate)
        }
    }

    private suspend fun getHighPriorityTasks(): List<Task> {
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        return allTasks.filter { !it.completed && it.priority == Priority.HIGH }
    }

    private suspend fun getTasksNearDeadline(hours: Int = 6): List<Task> {
        return getTasksDueSoon(hours)
    }

    private suspend fun getTodayTasks(): List<Task> {
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        val todayStart = today.time

        today.set(Calendar.HOUR_OF_DAY, 23)
        today.set(Calendar.MINUTE, 59)
        today.set(Calendar.SECOND, 59)
        val todayEnd = today.time

        return allTasks.filter { task ->
            !task.completed &&
            task.dueDate != null &&
            task.dueDate.after(todayStart) &&
            task.dueDate.before(todayEnd)
        }
    }

    private fun getStreakData(): StreakData {
        // This would typically be calculated from user data
        // For now, return mock data
        return StreakData(
            currentStreak = 5,
            longestStreak = 12,
            tasksCompletedToday = 3,
            lastCompletionDate = Date()
        )
    }

    private fun getWeeklyStats(): WeeklyStats {
        // This would typically be calculated from user data
        // For now, return mock data
        return WeeklyStats(
            tasksCompleted = 15,
            tasksCreated = 18,
            averageCompletionTime = 2.5,
            mostProductiveDay = "Wednesday",
            totalFocusTime = 25.5 // hours
        )
    }

    private fun getDayStats(): DayStats {
        // This would typically be calculated from user data
        // For now, return mock data
        return DayStats(
            tasksCompleted = 4,
            tasksRemaining = 3,
            totalFocusTime = 6.5,
            productivity = 85
        )
    }

    // ========== SCHEDULE CHECKING METHODS ==========

    /**
     * Check if a notification should be triggered based on schedule configuration
     */
    fun shouldTriggerNotification(type: NotificationType): Boolean {
        val preference = settingsManager.getNotificationPreference(type)
        val config = preference.scheduleConfig

        // Check if notification is enabled
        if (!preference.enabled) return false

        // Check quiet hours
        if (settingsManager.isInQuietHours()) return false

        // Check day of week
        val today = java.time.LocalDate.now().dayOfWeek
        if (!config.daysOfWeek.contains(today)) return false

        // Check custom times (if specified)
        if (config.customTimes.isNotEmpty()) {
            val currentTime = LocalTime.now()
            val shouldTrigger = config.customTimes.any { scheduledTime ->
                // Allow 5-minute window for triggering
                val timeDiff = java.time.Duration.between(scheduledTime, currentTime).toMinutes().toInt()
                timeDiff in -5..5
            }
            if (!shouldTrigger) return false
        }

        return true
    }

    /**
     * Get next trigger time for a notification type
     */
    fun getNextTriggerTime(type: NotificationType): Date? {
        val preference = settingsManager.getNotificationPreference(type)
        val config = preference.scheduleConfig

        if (!preference.enabled) return null

        val now = LocalDateTime.now()

        // If custom times are specified, find next one
        if (config.customTimes.isNotEmpty()) {
            val today = now.toLocalDate()
            val currentTime = now.toLocalTime()

            // Check today's custom times
            val nextToday = config.customTimes
                .filter { it.isAfter(currentTime) }
                .minOrNull()

            if (nextToday != null) {
                return LocalDateTime.of(today, nextToday)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .let { Date.from(it) }
            }

            // Check tomorrow's first custom time
            val tomorrow = today.plusDays(1)
            val firstTomorrow = config.customTimes.minOrNull()

            if (firstTomorrow != null) {
                return LocalDateTime.of(tomorrow, firstTomorrow)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .let { Date.from(it) }
            }
        }

        // For periodic notifications, calculate next interval
        if (config.intervalHours > 0) {
            return Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(config.intervalHours.toLong()))
        }

        return null
    }
}


