package com.saokt.taskmanager.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saokt.taskmanager.MainActivity
import com.saokt.taskmanager.R
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.NotificationPreference
import com.saokt.taskmanager.domain.model.NotificationPriority
import com.saokt.taskmanager.domain.model.StreakData
import com.saokt.taskmanager.domain.model.WeeklyStats
import com.saokt.taskmanager.domain.model.DayStats
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminder_channel"
        const val TASK_REMINDER_CHANNEL_NAME = "Task Reminders"
        const val TASK_REMINDER_CHANNEL_DESCRIPTION = "Notifications for task reminders"
        const val TASK_DUE_CHANNEL_ID = "task_due_channel"
        const val TASK_DUE_CHANNEL_NAME = "Task Due Alerts"
        const val TASK_DUE_CHANNEL_DESCRIPTION = "Notifications for tasks that are due soon"
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Task reminder channel
            val reminderChannel = NotificationChannel(
                TASK_REMINDER_CHANNEL_ID,
                TASK_REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = TASK_REMINDER_CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            // Task due channel (higher priority)
            val dueChannel = NotificationChannel(
                TASK_DUE_CHANNEL_ID,
                TASK_DUE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = TASK_DUE_CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(reminderChannel)
            systemNotificationManager.createNotificationChannel(dueChannel)
        }
    }

    fun showTaskReminderNotification(tasks: List<Task>) {
        if (tasks.isEmpty()) return

        val pendingIntent = createTaskListPendingIntent()

        val notification = when {
            tasks.size == 1 -> {
                val task = tasks.first()
                createSingleTaskReminderNotification(task, pendingIntent)
            }
            else -> {
                createMultipleTasksReminderNotification(tasks, pendingIntent)
            }
        }

        try {
            notificationManager.notify(generateNotificationId(), notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            e.printStackTrace()
        }
    }

    fun showTaskDueNotification(task: Task) {
        val pendingIntent = createTaskDetailPendingIntent(task.id)
        
        val dueText = task.dueDate?.let { dueDate ->
            "Due: ${dateFormatter.format(dueDate)}"
        } ?: "Due soon"

        val notification = NotificationCompat.Builder(context, TASK_DUE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Task Due Soon! ⏰")
            .setContentText("${task.title} - $dueText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}\n\n$dueText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Complete",
                createMarkCompleteAction(task.id)
            )
            .build()

        try {
            notificationManager.notify(task.id.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createSingleTaskReminderNotification(
        task: Task,
        pendingIntent: PendingIntent
    ): android.app.Notification {
        val dueText = task.dueDate?.let { dueDate ->
            "\nDue: ${dateFormatter.format(dueDate)}"
        } ?: ""

        return NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Don't forget your task! 📝")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}$dueText"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Complete",
                createMarkCompleteAction(task.id)
            )
            .build()
    }

    private fun createMultipleTasksReminderNotification(
        tasks: List<Task>,
        pendingIntent: PendingIntent
    ): android.app.Notification {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("You have ${tasks.size} pending tasks")
            .setSummaryText("Tap to view all tasks")

        tasks.take(5).forEach { task ->
            val dueText = task.dueDate?.let { " (Due: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(it)})" } ?: ""
            inboxStyle.addLine("• ${task.title}$dueText")
        }

        if (tasks.size > 5) {
            inboxStyle.addLine("... and ${tasks.size - 5} more")
        }

        return NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Task Reminder 📝")
            .setContentText("You have ${tasks.size} pending tasks")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setNumber(tasks.size)
            .build()
    }

    private fun createTaskListPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createTaskDetailPendingIntent(taskId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }
        
        return PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createMarkCompleteAction(taskId: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }

    fun cancelTaskNotification(taskId: String) {
        notificationManager.cancel(taskId.hashCode())
    }

    fun cancelAllTaskNotifications() {
        notificationManager.cancelAll()
    }

    // ========== ENHANCED NOTIFICATION METHODS ==========

    fun showEnhancedTaskReminderNotification(tasks: List<Task>, preference: NotificationPreference) {
        if (tasks.isEmpty()) return

        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskListPendingIntent()

        val notification = when {
            tasks.size == 1 -> {
                val task = tasks.first()
                createEnhancedSingleTaskNotification(task, preference, pendingIntent, channelId)
            }
            else -> {
                createEnhancedMultipleTasksNotification(tasks, preference, pendingIntent, channelId)
            }
        }

        try {
            notificationManager.notify(generateNotificationId(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedDueSoonNotification(task: Task, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskDetailPendingIntent(task.id)

        val dueText = task.dueDate?.let { dueDate ->
            "Due: ${dateFormatter.format(dueDate)}"
        } ?: "Due soon"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Task Due Soon!")
            .setContentText("${task.title} - $dueText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}\n\n$dueText"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(android.graphics.Color.parseColor(preference.ledColor), 1000, 1000)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Complete",
                createMarkCompleteAction(task.id)
            )
            .build()

        try {
            notificationManager.notify(task.id.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedOverdueNotification(task: Task, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskDetailPendingIntent(task.id)

        val overdueText = task.dueDate?.let { dueDate ->
            "Overdue by ${getOverdueTime(dueDate)}"
        } ?: "Overdue"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Task Overdue!")
            .setContentText("${task.title} - $overdueText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}\n\n$overdueText"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .apply {
                if (preference.ledEnabled) {
                    setLights(android.graphics.Color.RED, 500, 500)
                }
            }
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Complete",
                createMarkCompleteAction(task.id)
            )
            .build()

        try {
            notificationManager.notify(task.id.hashCode() + 1000, notification) // Different ID for overdue
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedHighPriorityNotification(tasks: List<Task>, preference: NotificationPreference) {
        if (tasks.isEmpty()) return

        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskListPendingIntent()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 High Priority Tasks")
            .setContentText("You have ${tasks.size} high priority task(s) to complete")
            .setStyle(NotificationCompat.InboxStyle().also { style ->
                style.setBigContentTitle("High Priority Tasks")
                style.setSummaryText("Tap to view all tasks")
                tasks.take(5).forEach { task ->
                    val dueText = task.dueDate?.let { " (Due: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(it)})" } ?: ""
                    style.addLine("• ${task.title}$dueText")
                }
                if (tasks.size > 5) {
                    style.addLine("... and ${tasks.size - 5} more")
                }
            })
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setNumber(tasks.size)
            .apply {
                if (preference.ledEnabled) {
                    setLights(android.graphics.Color.parseColor("#FF6B35"), 1000, 1000) // Orange color
                }
            }
            .build()

        try {
            notificationManager.notify(generateNotificationId() + 2000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedProjectUpdateNotification(project: Project, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createProjectDetailPendingIntent(project.id)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📁 Project Update")
            .setContentText("${project.title} has been updated")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${project.title}\n\n${project.description ?: "No description"}"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(project.id.hashCode() + 3000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedAssignmentNotification(task: Task, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskDetailPendingIntent(task.id)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("👤 New Task Assignment")
            .setContentText("You've been assigned: ${task.title}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}\n\nAssigned by: ${task.assignedBy ?: "Unknown"}"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(task.id.hashCode() + 4000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedCelebrationNotification(task: Task, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskDetailPendingIntent(task.id)

        val celebrationMessage = getRandomCelebrationMessage()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 Task Completed!")
            .setContentText("$celebrationMessage - ${task.title}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$celebrationMessage\n\nYou've successfully completed:\n${task.title}"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(task.id.hashCode() + 5000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedStreakNotification(streakData: StreakData, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createAppLaunchPendingIntent()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 Keep Your Streak Going!")
            .setContentText("Current streak: ${streakData.currentStreak} days")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You're on a ${streakData.currentStreak} day streak!\n\n" +
                        "Tasks completed today: ${streakData.tasksCompletedToday}\n" +
                        "Longest streak: ${streakData.longestStreak} days\n\n" +
                        "Complete a task today to keep your momentum!"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(6000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedDeadlineWarningNotification(task: Task, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskDetailPendingIntent(task.id)

        val timeLeft = task.dueDate?.let { getTimeLeftText(it) } ?: "Soon"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ Deadline Approaching!")
            .setContentText("${task.title} - Due $timeLeft")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}\n\nDue $timeLeft"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .apply {
                if (preference.ledEnabled) {
                    setLights(android.graphics.Color.YELLOW, 1000, 1000)
                }
            }
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Complete",
                createMarkCompleteAction(task.id)
            )
            .build()

        try {
            notificationManager.notify(task.id.hashCode() + 7000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedWeeklySummaryNotification(stats: WeeklyStats, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createAppLaunchPendingIntent()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📊 Your Weekly Summary")
            .setContentText("Tasks completed: ${stats.tasksCompleted}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("📈 This Week's Performance:\n\n" +
                        "✅ Tasks completed: ${stats.tasksCompleted}\n" +
                        "📝 Tasks created: ${stats.tasksCreated}\n" +
                        "⏱️ Avg. completion time: ${stats.averageCompletionTime}h\n" +
                        "🎯 Most productive day: ${stats.mostProductiveDay}\n" +
                        "🕐 Total focus time: ${stats.totalFocusTime}h\n\n" +
                        "Keep up the great work!"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(8000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedMorningBriefingNotification(tasks: List<Task>, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createTaskListPendingIntent()

        val greeting = getMorningGreeting()
        val taskCount = tasks.size

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$greeting Good morning!")
            .setContentText("You have $taskCount task(s) scheduled for today")
            .setStyle(NotificationCompat.InboxStyle().also { style ->
                style.setBigContentTitle("$greeting Here's your day:")
                style.setSummaryText("$taskCount tasks to complete")
                tasks.take(3).forEach { task ->
                    val timeText = task.dueDate?.let { " (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)})" } ?: ""
                    style.addLine("• ${task.title}$timeText")
                }
                if (tasks.size > 3) {
                    style.addLine("... and ${tasks.size - 3} more")
                }
            })
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(9000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showEnhancedEveningReviewNotification(stats: DayStats, preference: NotificationPreference) {
        val channelId = getChannelIdForPriority(preference.priority)
        val pendingIntent = createAppLaunchPendingIntent()

        val reviewMessage = getEveningReviewMessage(stats)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🌙 Evening Review")
            .setContentText(reviewMessage)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$reviewMessage\n\n" +
                        "📊 Today's Stats:\n" +
                        "✅ Completed: ${stats.tasksCompleted}\n" +
                        "⏳ Remaining: ${stats.tasksRemaining}\n" +
                        "🕐 Focus time: ${stats.totalFocusTime}h\n" +
                        "📈 Productivity: ${stats.productivity}%\n\n" +
                        "Great job today! Rest well."))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        try {
            notificationManager.notify(10000, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createEnhancedSingleTaskNotification(
        task: Task,
        preference: NotificationPreference,
        pendingIntent: PendingIntent,
        channelId: String
    ): android.app.Notification {
        val dueText = task.dueDate?.let { dueDate ->
            "\nDue: ${dateFormatter.format(dueDate)}"
        } ?: ""

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📝 Task Reminder")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\n\n${task.description}$dueText"))
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(android.graphics.Color.parseColor(preference.ledColor), 1000, 1000)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark Complete",
                createMarkCompleteAction(task.id)
            )
            .build()
    }

    private fun createEnhancedMultipleTasksNotification(
        tasks: List<Task>,
        preference: NotificationPreference,
        pendingIntent: PendingIntent,
        channelId: String
    ): android.app.Notification {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("You have ${tasks.size} pending tasks")
            .setSummaryText("Tap to view all tasks")

        tasks.take(5).forEach { task ->
            val dueText = task.dueDate?.let { " (Due: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(it)})" } ?: ""
            inboxStyle.addLine("• ${task.title}$dueText")
        }

        if (tasks.size > 5) {
            inboxStyle.addLine("... and ${tasks.size - 5} more")
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📝 Task Reminder")
            .setContentText("You have ${tasks.size} pending tasks")
            .setStyle(inboxStyle)
            .setPriority(getNotificationPriority(preference.priority))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setNumber(tasks.size)
            .apply {
                if (preference.ledEnabled) {
                    setLights(android.graphics.Color.parseColor(preference.ledColor), 1000, 1000)
                }
            }
            .build()
    }

    private fun getChannelIdForPriority(priority: NotificationPriority): String {
        return when (priority) {
            NotificationPriority.URGENT -> TASK_DUE_CHANNEL_ID
            NotificationPriority.HIGH -> TASK_DUE_CHANNEL_ID
            NotificationPriority.MEDIUM -> TASK_REMINDER_CHANNEL_ID
            NotificationPriority.LOW -> TASK_REMINDER_CHANNEL_ID
        }
    }

    private fun getNotificationPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        }
    }

    private fun createProjectDetailPendingIntent(projectId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("projectId", projectId)
        }

        return PendingIntent.getActivity(
            context,
            projectId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAppLaunchPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOverdueTime(dueDate: Date): String {
        val diff = System.currentTimeMillis() - dueDate.time
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""}"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
            else -> "a few minutes"
        }
    }

    private fun getTimeLeftText(dueDate: Date): String {
        val diff = dueDate.time - System.currentTimeMillis()
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

        return when {
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
            else -> "now"
        }
    }

    private fun getRandomCelebrationMessage(): String {
        val messages = listOf(
            "Awesome work! 🎉",
            "Great job! ⭐",
            "Well done! 🌟",
            "Fantastic! ✨",
            "Excellent! 🏆",
            "Superb! 🎊",
            "Outstanding! 🌈",
            "Brilliant! 💫"
        )
        return messages.random()
    }

    private fun getMorningGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "🌅 Good morning"
            in 12..16 -> "☀️ Good afternoon"
            else -> "🌆 Good evening"
        }
    }

    private fun getEveningReviewMessage(stats: DayStats): String {
        return when {
            stats.productivity >= 90 -> "🌟 Exceptional day! You crushed it!"
            stats.productivity >= 75 -> "👍 Great job today! Well done!"
            stats.productivity >= 50 -> "👌 Solid day! Keep it up!"
            else -> "💪 Every day is a new opportunity!"
        }
    }
}
