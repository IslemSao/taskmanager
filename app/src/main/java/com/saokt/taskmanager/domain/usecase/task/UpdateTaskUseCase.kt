package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        if (task.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty"))
        }

        val currentUser = userRepository.getCurrentUser().first() 
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        
        // Update the task with current timestamp and set assignedBy if task is assigned
        val updatedTask = task.copy(
            modifiedAt = Date(),
            assignedBy = if (task.assignedTo != null && task.assignedBy.isNullOrBlank()) currentUser.id else task.assignedBy
        )
        
        return taskRepository.updateTask(updatedTask)
    }
}
