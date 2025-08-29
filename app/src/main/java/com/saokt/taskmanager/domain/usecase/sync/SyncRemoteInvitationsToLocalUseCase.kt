package com.saokt.taskmanager.domain.usecase.sync

import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteInvitationsToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(invitations: List<ProjectInvitationDto>) {
        userRepository.syncRemoteInvitationsToLocal(invitations)
    }
} 