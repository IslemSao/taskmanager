package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskComplitionUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        if (task.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty"))
        }
        return taskRepository.toggleTaskcomplition(task)
    }
}
