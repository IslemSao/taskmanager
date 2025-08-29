package com.saokt.taskmanager.domain.usecase.remote

import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListenToRemoteProjectsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>> {
        return userRepository.listenToRemoteProjects()
    }
} 