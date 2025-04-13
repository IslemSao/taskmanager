package com.example.taskmanager.domain.usecase.sync

import com.example.taskmanager.data.remote.dto.ProjectInvitationDto
import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteInvitationsToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(invitations: List<ProjectInvitationDto>) {
        userRepository.syncRemoteInvitationsToLocal(invitations)
    }
} 