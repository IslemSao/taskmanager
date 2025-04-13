package com.example.taskmanager.domain.usecase.remote

import com.example.taskmanager.data.remote.dto.TaskDto
import com.example.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListenToRemoteTasksUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Result<List<TaskDto>>> {
        return userRepository.listenToRemoteTasks()
    }
} 