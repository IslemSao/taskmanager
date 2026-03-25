package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.canonicalized
import com.saokt.taskmanager.domain.repository.TaskRepository
import java.util.Date
import javax.inject.Inject

class MoveTaskToStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task, status: TaskStatus): Result<Task> {
        val updatedTask = task.copy(
            status = status,
            completed = status == TaskStatus.DONE,
            modifiedAt = Date()
        ).canonicalized()
        return taskRepository.updateTask(updatedTask)
    }
}
