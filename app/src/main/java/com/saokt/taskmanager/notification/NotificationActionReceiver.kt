package com.saokt.taskmanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.model.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository
    
    @Inject
    lateinit var notificationManager: TaskNotificationManager

    companion object {
        const val ACTION_MARK_COMPLETE = "com.saokt.taskmanager.ACTION_MARK_COMPLETE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_MARK_COMPLETE -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    markTaskComplete(taskId)
                }
            }
        }
    }

    private fun markTaskComplete(taskId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the task first
                taskRepository.getTaskById(taskId).collect { task ->
                    if (task != null && !task.completed) {
                        // Mark task as complete
                        val updatedTask = task.copy(
                            completed = true,
                            modifiedAt = Date()
                        )
                        taskRepository.updateTask(updatedTask)
                        
                        // Cancel the notification
                        notificationManager.cancelTaskNotification(taskId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
