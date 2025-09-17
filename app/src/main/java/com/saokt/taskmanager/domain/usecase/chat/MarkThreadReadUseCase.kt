package com.saokt.taskmanager.domain.usecase.chat

import com.saokt.taskmanager.domain.repository.ChatRepository
import javax.inject.Inject

class MarkThreadReadUseCase @Inject constructor(
    private val repo: ChatRepository
) {
    suspend operator fun invoke(threadId: String, userId: String): Result<Unit> = repo.markAsRead(threadId, userId)
}
