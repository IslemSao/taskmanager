package com.example.taskmanager.domain.usecase.sync

import com.example.taskmanager.data.remote.dto.TaskDto
import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteTasksToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(tasks: List<TaskDto>) {
        userRepository.syncRemoteTasksToLocal(tasks)
    }
} 