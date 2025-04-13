package com.example.taskmanager.domain.usecase.sync

import com.example.taskmanager.data.remote.dto.ProjectMemberDto
import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteMembersToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(members: List<ProjectMemberDto>) {
        userRepository.syncRemoteMembersToLocal(members)
    }
} 