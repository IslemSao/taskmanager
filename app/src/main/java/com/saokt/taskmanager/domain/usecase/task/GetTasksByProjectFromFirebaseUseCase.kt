package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import javax.inject.Inject

class GetTasksByProjectFromFirebaseUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(projectId: String): Result<List<Task>> {
        return taskRepository.getTasksByProjectFromFirebase(projectId)
    }
} 