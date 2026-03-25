package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskTimelineEngine
import com.saokt.taskmanager.domain.repository.TaskRepository
import javax.inject.Inject

class RescheduleTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task, deltaDays: Long): Result<Task> {
        return taskRepository.updateTask(
            TaskTimelineEngine.shiftTask(task, deltaDays)
        )
    }
}
