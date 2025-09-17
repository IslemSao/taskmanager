package com.saokt.taskmanager.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.model.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationManager: TaskNotificationManager,
    private val settingsManager: NotificationSettingsManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "task_reminder_work"
        const val INPUT_DATA_REMINDER_TYPE = "reminder_type"
        const val REMINDER_TYPE_PENDING = "pending"
        const val REMINDER_TYPE_DUE_SOON = "due_soon"
    }

    override suspend fun doWork(): Result {
        return try {
            val reminderType = inputData.getString(INPUT_DATA_REMINDER_TYPE) ?: REMINDER_TYPE_PENDING
            
            when (reminderType) {
                REMINDER_TYPE_PENDING -> checkPendingTasks()
                REMINDER_TYPE_DUE_SOON -> checkTasksDueSoon()
                else -> checkPendingTasks()
            }
            
            Result.success()
        } catch (exception: Exception) {
            exception.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun checkPendingTasks() {
        // Check if notifications are enabled and not in quiet hours
        if (!settingsManager.areRemindersEnabled) {
            android.util.Log.d("TaskReminderWorker", "Reminders disabled, skipping notification")
            return
        }
        
        if (settingsManager.isInQuietHours()) {
            android.util.Log.d("TaskReminderWorker", "In quiet hours, skipping notification")
            return
        }
        
        // Get all incomplete tasks
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        val incompleteTasks = allTasks.filter { !it.completed }
        
        if (incompleteTasks.isNotEmpty()) {
            // Filter current tasks (tasks that are active and not overdue by more than 7 days)
            val currentDate = Date()
            val weekAgo = Date(currentDate.time - TimeUnit.DAYS.toMillis(7))
            
            val currentTasks = incompleteTasks.filter { task ->
                task.dueDate?.let { dueDate ->
                    dueDate.after(weekAgo) // Not overdue by more than a week
                } ?: true // Include tasks without due dates
            }
            
            if (currentTasks.isNotEmpty()) {
                notificationManager.showTaskReminderNotification(currentTasks)
            }
        }
    }

    private suspend fun checkTasksDueSoon() {
        // Check if notifications are enabled and not in quiet hours
        if (!settingsManager.areDueSoonNotificationsEnabled) {
            android.util.Log.d("TaskReminderWorker", "Due soon notifications disabled, skipping notification")
            return
        }
        
        if (settingsManager.isInQuietHours()) {
            android.util.Log.d("TaskReminderWorker", "In quiet hours, skipping due soon notification")
            return
        }
        
        val allTasks = taskRepository.getAllTasks().firstOrNull() ?: emptyList()
        val currentDate = Date()
        val next24Hours = Date(currentDate.time + TimeUnit.HOURS.toMillis(24))
        
        // Find tasks due in the next 24 hours
        val tasksDueSoon = allTasks.filter { task ->
            !task.completed && 
            task.dueDate != null && 
            task.dueDate.after(currentDate) && 
            task.dueDate.before(next24Hours)
        }
        
        tasksDueSoon.forEach { task ->
            notificationManager.showTaskDueNotification(task)
        }
    }
}
