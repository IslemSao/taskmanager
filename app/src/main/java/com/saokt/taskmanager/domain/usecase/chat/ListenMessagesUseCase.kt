package com.saokt.taskmanager.domain.usecase.chat

import com.saokt.taskmanager.domain.model.ChatMessage
import com.saokt.taskmanager.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ListenMessagesUseCase @Inject constructor(
    private val repo: ChatRepository
) {
    operator fun invoke(threadId: String): Flow<List<ChatMessage>> = repo.listenMessages(threadId)
}
