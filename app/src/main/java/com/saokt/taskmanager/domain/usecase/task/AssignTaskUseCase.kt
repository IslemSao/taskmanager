package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date
import javax.inject.Inject

class AssignTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        taskId: String,
        assignedToUserId: String,
        assignedByUserId: String
    ): Result<Task> {
        return try {
            val currentTask = taskRepository.getTaskById(taskId).firstOrNull()
                ?: return Result.failure(IllegalStateException("Task not found"))

            val updatedTask = currentTask.copy(
                assignedTo = assignedToUserId,
                assignedAt = Date(),
                assignedBy = assignedByUserId,
                modifiedAt = Date()
            )

            taskRepository.updateTask(updatedTask)
            Result.success(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 