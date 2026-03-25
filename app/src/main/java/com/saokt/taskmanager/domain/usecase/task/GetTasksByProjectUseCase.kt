package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksByProjectUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(projectId: String): Flow<List<Task>> {
        return taskRepository.getTasksByProject(projectId)
    }
}
