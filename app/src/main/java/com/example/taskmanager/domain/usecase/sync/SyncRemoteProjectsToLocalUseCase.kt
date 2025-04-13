package com.example.taskmanager.domain.usecase.sync

import com.example.taskmanager.data.remote.dto.ProjectDto
import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteProjectsToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(projects: List<ProjectDto>) {
        userRepository.syncRemoteProjectsToLocal(projects)
    }
} 