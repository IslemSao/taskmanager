package com.example.taskmanager.domain.usecase.task

import android.util.Log
import com.example.taskmanager.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        Log.d("bombardiro" , "DeleteTaskUseCase: $taskId")

        return taskRepository.deleteTask(taskId)
    }
}
