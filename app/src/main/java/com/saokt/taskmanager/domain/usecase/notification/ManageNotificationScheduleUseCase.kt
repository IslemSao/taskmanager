package com.saokt.taskmanager.domain.usecase.notification

import com.saokt.taskmanager.notification.TaskNotificationScheduler
import com.saokt.taskmanager.notification.NotificationSettingsManager
import javax.inject.Inject

class ManageNotificationScheduleUseCase @Inject constructor(
    private val notificationScheduler: TaskNotificationScheduler,
    private val settingsManager: NotificationSettingsManager
) {
    
    /**
     * Start notification scheduling based on user preferences
     */
    suspend operator fun invoke(enable: Boolean) {
        if (enable && settingsManager.areRemindersEnabled) {
            notificationScheduler.startNotificationScheduling()
        } else {
            notificationScheduler.cancelAllReminders()
        }
    }

    /**
     * Update notification settings and reschedule if needed
     */
    suspend fun updateSettings(
        remindersEnabled: Boolean,
        dueSoonEnabled: Boolean,
        reminderInterval: Int? = null,
        dueSoonInterval: Int? = null
    ) {
        // Update settings
        settingsManager.areRemindersEnabled = remindersEnabled
        settingsManager.areDueSoonNotificationsEnabled = dueSoonEnabled
        
        reminderInterval?.let { 
            settingsManager.reminderIntervalHours = it 
        }
        dueSoonInterval?.let { 
            settingsManager.dueSoonIntervalHours = it 
        }

        // Reschedule based on new settings
        notificationScheduler.cancelAllReminders()
        
        if (remindersEnabled) {
            notificationScheduler.scheduleTaskReminders()
        }
        
        if (dueSoonEnabled) {
            notificationScheduler.scheduleDueSoonReminders()
        }
    }

    /**
     * Check if notifications are currently scheduled
     */
    fun areNotificationsScheduled(): Boolean {
        return notificationScheduler.areRemindersScheduled()
    }

    /**
     * Trigger immediate notification check
     */
    suspend fun triggerImmediateCheck() {
        if (settingsManager.areRemindersEnabled) {
            notificationScheduler.scheduleImmediateReminder()
        }
    }
}
