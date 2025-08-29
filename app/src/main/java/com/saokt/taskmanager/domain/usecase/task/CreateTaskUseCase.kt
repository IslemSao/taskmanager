package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        if (task.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty"))
        }
        
        val currentUser = userRepository.getCurrentUser().first() 
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        
        // Set createdBy to current user if not set
        val taskWithUser = task.copy(
            createdBy = if (task.createdBy.isBlank()) currentUser.id else task.createdBy
        )
        
        return taskRepository.createTask(taskWithUser)
    }
}
