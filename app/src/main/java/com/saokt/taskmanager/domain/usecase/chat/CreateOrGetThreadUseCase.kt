package com.saokt.taskmanager.domain.usecase.chat

import com.saokt.taskmanager.domain.model.ChatThread
import com.saokt.taskmanager.domain.repository.ChatRepository
import javax.inject.Inject

class CreateOrGetThreadUseCase @Inject constructor(
    private val repo: ChatRepository
) {
    suspend operator fun invoke(projectId: String, taskId: String?, participantIds: List<String>): Result<ChatThread> =
        repo.createOrGetThread(projectId, taskId, participantIds)
}
