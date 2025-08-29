package com.saokt.taskmanager.domain.usecase.sync

import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SyncRemoteMembersToLocalUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(members: List<ProjectMemberDto>) {
        userRepository.syncRemoteMembersToLocal(members)
    }
} 