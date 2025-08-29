package com.saokt.taskmanager.notification

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val TASK_REMINDER_WORK_NAME = "task_reminder_periodic_work"
        const val DUE_SOON_REMINDER_WORK_NAME = "due_soon_reminder_periodic_work"
        private const val REMINDER_INTERVAL_HOURS = 2L
        private const val DUE_SOON_CHECK_INTERVAL_HOURS = 6L
    }

    /**
     * Schedule periodic task reminders every 2 hours
     */
    fun scheduleTaskReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val reminderWorkRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            REMINDER_INTERVAL_HOURS, TimeUnit.HOURS,
            30, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    TaskReminderWorker.INPUT_DATA_REMINDER_TYPE to TaskReminderWorker.REMINDER_TYPE_PENDING
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TASK_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWorkRequest
        )
    }

    /**
     * Schedule periodic checks for tasks due soon (every 6 hours)
     */
    fun scheduleDueSoonReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val dueSoonWorkRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            DUE_SOON_CHECK_INTERVAL_HOURS, TimeUnit.HOURS,
            1, TimeUnit.HOURS // Flex interval
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    TaskReminderWorker.INPUT_DATA_REMINDER_TYPE to TaskReminderWorker.REMINDER_TYPE_DUE_SOON
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DUE_SOON_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dueSoonWorkRequest
        )
    }

    /**
     * Schedule an immediate one-time reminder check
     */
    fun scheduleImmediateReminder() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val immediateWorkRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    TaskReminderWorker.INPUT_DATA_REMINDER_TYPE to TaskReminderWorker.REMINDER_TYPE_PENDING
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
    }

    /**
     * Cancel all scheduled reminders
     */
    fun cancelAllReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(TASK_REMINDER_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(DUE_SOON_REMINDER_WORK_NAME)
    }

    /**
     * Cancel only task reminders (but keep due soon reminders)
     */
    fun cancelTaskReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(TASK_REMINDER_WORK_NAME)
    }

    /**
     * Cancel only due soon reminders
     */
    fun cancelDueSoonReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(DUE_SOON_REMINDER_WORK_NAME)
    }

    /**
     * Start all notification scheduling
     */
    fun startNotificationScheduling() {
        scheduleTaskReminders()
        scheduleDueSoonReminders()
    }

    /**
     * Check if reminders are currently scheduled
     */
    fun areRemindersScheduled(): Boolean {
        val reminderWorkInfo = WorkManager.getInstance(context).getWorkInfosForUniqueWork(TASK_REMINDER_WORK_NAME)
        val dueSoonWorkInfo = WorkManager.getInstance(context).getWorkInfosForUniqueWork(DUE_SOON_REMINDER_WORK_NAME)
        
        return try {
            val reminderActive = reminderWorkInfo.get().any { 
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING 
            }
            val dueSoonActive = dueSoonWorkInfo.get().any { 
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING 
            }
            reminderActive && dueSoonActive
        } catch (e: Exception) {
            false
        }
    }
}
