package com.saokt.taskmanager.domain.usecase.sync

import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteProjectsToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(projects: List<ProjectDto>) {
        userRepository.syncRemoteProjectsToLocal(projects)
    }
} 