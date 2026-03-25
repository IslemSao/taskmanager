package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> = taskRepository.deleteTask(taskId)
}
