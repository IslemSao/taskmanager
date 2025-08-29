package com.saokt.taskmanager.widget

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.text.Text
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.saokt.taskmanager.MainActivity
import com.saokt.taskmanager.R
import com.saokt.taskmanager.data.local.TaskManagerDatabase
import com.saokt.taskmanager.data.local.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load tasks off the main thread first
        val tasks: List<TaskEntity> = try {
            withContext(Dispatchers.IO) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    TaskManagerDatabase::class.java,
                    "task_manager_db"
                ).build()
                try {
                    db.taskDao().getTopImportantUncompleted(2)
                } finally {
                    db.close()
                }
            }
        } catch (t: Throwable) {
            Log.e("TaskWidget", "Failed to load tasks for widget", t)
            emptyList()
        }

        try {
            provideContent { TaskWidgetContent(tasks) }
        } catch (e: Exception) {
            Log.e("TaskWidget", "Failed to provide widget content", e)
            provideContent { 
                ErrorWidgetContent("Unable to load tasks")
            }
        }
    }
}

@Composable
private fun TaskWidgetContent(tasks: List<TaskEntity>) {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier
            .background(ImageProvider(R.color.widget_bg))
            .cornerRadius(R.dimen.widget_corner_radius)
    ) {
        // Header with gradient-like background
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(R.color.widget_header_bg))
                .padding(R.dimen.widget_header_padding)
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher_round),
                contentDescription = "App icon",
                modifier = GlanceModifier.size(R.dimen.widget_header_icon)
            )
            Spacer(modifier = GlanceModifier.size(R.dimen.widget_header_spacing))
            Text(
                text = "Top Tasks",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_text_white)
                )
            )
            Spacer(modifier = GlanceModifier.size(R.dimen.widget_header_spacing))
            Text(
                text = todayLabel(),
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_white)
                )
            )
        }
        
        // Tasks content with better spacing
        Column(modifier = GlanceModifier.padding(R.dimen.widget_padding)) {
            if (tasks.isEmpty()) {
                Text(
                    text = "No pending tasks",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_secondary)
                    )
                )
            } else {
                tasks.take(2).forEachIndexed { index, task ->
                    if (index > 0) {
                        Spacer(modifier = GlanceModifier.size(R.dimen.widget_item_spacing))
                    }
                    
                    // Enhanced task card
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(R.color.widget_card_bg))
                            .cornerRadius(R.dimen.widget_spacing_small)
                            .padding(R.dimen.widget_card_padding)
                            .clickable(actionStartActivity(ComponentName(ctx, MainActivity::class.java)))
                    ) {
                        // Title row with priority
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                text = "${priorityEmoji(task)}  ${task.title}",
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    color = ColorProvider(R.color.widget_text_primary)
                                )
                            )
                        }
                        
                        // Description (if any)
                        task.description.takeIf { it.isNotBlank() }?.let { desc ->
                            Spacer(modifier = GlanceModifier.size(R.dimen.widget_spacing_small))
                            Text(
                                text = truncate(desc, 80),
                                style = TextStyle(
                                    color = ColorProvider(R.color.widget_text_secondary)
                                )
                            )
                        }
                        
                        // Meta row: due + assigned flag
                        val due = task.dueDate?.let { formatDueLabel(it) }
                        val assigned = if (task.assignedTo != null) "Assigned" else null
                        if (due != null || assigned != null) {
                            Spacer(modifier = GlanceModifier.size(R.dimen.widget_spacing_small))
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                val parts = listOfNotNull(due, assigned)
                                Text(
                                    text = parts.joinToString("  •  "),
                                    style = TextStyle(
                                        color = ColorProvider(R.color.widget_text_secondary)
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = GlanceModifier.size(R.dimen.widget_list_spacing))
                
                // Footer with better styling
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(ComponentName(ctx, MainActivity::class.java)))
                ) {
                    Text(
                        text = "Open app →",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(R.color.widget_header_bg)
                        )
                    )
                }
            }
        }
    }
}

private fun priorityEmoji(task: TaskEntity): String = when (task.priority.name.uppercase()) {
    "HIGH" -> "🔴"
    "MEDIUM" -> "🟡"
    else -> "🟢"
}

private fun formatDueLabel(date: Date): String {
    return try {
        // Use Calendar for better Android compatibility instead of LocalDate.ofInstant
        val calendar = java.util.Calendar.getInstance().apply {
            time = date
        }
        val today = java.util.Calendar.getInstance()
        val tomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, tomorrow) -> "Tomorrow"
            else -> {
                val format = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                format.format(date)
            }
        }
    } catch (e: Exception) {
        "Due date"
    }
}

private fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun todayLabel(): String {
    return try {
        val format = java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault())
        format.format(java.util.Date())
    } catch (e: Exception) {
        "Today"
    }
}

private fun truncate(text: String, max: Int): String =
    if (text.length <= max) text else text.take(max - 1) + "…"

@Composable
private fun ErrorWidgetContent(message: String) {
    Column(
        modifier = GlanceModifier
            .background(ImageProvider(R.color.widget_bg))
            .cornerRadius(R.dimen.widget_corner_radius)
            .padding(R.dimen.widget_padding)
    ) {
        Text(
            text = "Task Manager",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_text_primary)
            )
        )
        Spacer(modifier = GlanceModifier.size(R.dimen.widget_spacing_small))
        Text(
            text = message,
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_secondary)
            )
        )
        Spacer(modifier = GlanceModifier.size(R.dimen.widget_spacing_small))
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(ComponentName(LocalContext.current, MainActivity::class.java)))
        ) {
            Text(
                text = "Open app →",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(R.color.widget_header_bg)
                )
            )
        }
    }
}
