package com.saokt.taskmanager.domain.usecase.chat

import com.saokt.taskmanager.domain.model.ChatMessage
import com.saokt.taskmanager.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repo: ChatRepository
) {
    suspend operator fun invoke(threadId: String, message: ChatMessage): Result<Unit> = repo.sendMessage(threadId, message)
}
