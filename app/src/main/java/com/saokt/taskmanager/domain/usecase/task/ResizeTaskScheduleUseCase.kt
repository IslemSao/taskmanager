package com.saokt.taskmanager.domain.usecase.task

import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskTimelineEngine
import com.saokt.taskmanager.domain.model.TimelineEdge
import com.saokt.taskmanager.domain.repository.TaskRepository
import javax.inject.Inject

class ResizeTaskScheduleUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task, edge: TimelineEdge, deltaDays: Long): Result<Task> {
        return taskRepository.updateTask(
            TaskTimelineEngine.resizeTask(task, edge, deltaDays)
        )
    }
}
