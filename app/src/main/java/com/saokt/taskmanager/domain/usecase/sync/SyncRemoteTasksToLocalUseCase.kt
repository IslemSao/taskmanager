package com.saokt.taskmanager.domain.usecase.sync

import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteTasksToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(tasks: List<TaskDto>) {
        userRepository.syncRemoteTasksToLocal(tasks)
    }
} 