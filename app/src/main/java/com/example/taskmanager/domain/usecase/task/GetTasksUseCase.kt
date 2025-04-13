package com.example.taskmanager.domain.usecase.task

import com.example.taskmanager.data.local.dao.TaskDao
import com.example.taskmanager.domain.model.Priority
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return taskRepository.getAllTasks().map { entities ->
            entities.map { entity ->
                Task(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    completed = entity.completed,
                    createdAt = entity.createdAt,
                    labels = entity.labels,
                    priority = entity.priority,
                    projectId = entity.projectId,
                    subtasks = entity.subtasks,
                    dueDate = entity.dueDate,
                    modifiedAt = entity.modifiedAt,
                    syncStatus = entity.syncStatus
                )
            }.sortedWith(compareBy<Task> { it.completed }.thenBy { calculateTaskScore(it) })
        }
    }

    private fun calculateTaskScore(task: Task): Double {
        if (task.completed) {
            return Double.MAX_VALUE // Push completed tasks to the bottom
        }

        val priorityWeight = when (task.priority) {
            Priority.HIGH -> 3.0
            Priority.MEDIUM -> 2.0
            Priority.LOW -> 1.0
            else -> 1.0 // default case if priority is null
        }

        val now = Calendar.getInstance()
        val dueDate = task.dueDate ?: run {
            val defaultDue = Calendar.getInstance()
            defaultDue.add(Calendar.WEEK_OF_YEAR, 2)
            defaultDue.time
        }

        val dueCalendar = Calendar.getInstance().apply { time = dueDate }
        val daysUntilDue = daysBetween(now, dueCalendar).toDouble()

        // The score formula: lower score means higher priority
        return abs(daysUntilDue) / priorityWeight
    }

    private fun daysBetween(start: Calendar, end: Calendar): Long {
        val startDate = start.clone() as Calendar
        startDate.set(Calendar.HOUR_OF_DAY, 0)
        startDate.set(Calendar.MINUTE, 0)
        startDate.set(Calendar.SECOND, 0)
        startDate.set(Calendar.MILLISECOND, 0)

        val endDate = end.clone() as Calendar
        endDate.set(Calendar.HOUR_OF_DAY, 0)
        endDate.set(Calendar.MINUTE, 0)
        endDate.set(Calendar.SECOND, 0)
        endDate.set(Calendar.MILLISECOND, 0)

        val diffInMillis = endDate.timeInMillis - startDate.timeInMillis
        return diffInMillis / (1000 * 60 * 60 * 24)
    }
}