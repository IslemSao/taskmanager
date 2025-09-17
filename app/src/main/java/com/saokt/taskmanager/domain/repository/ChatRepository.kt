package com.saokt.taskmanager.domain.repository

import com.saokt.taskmanager.domain.model.ChatMessage
import com.saokt.taskmanager.domain.model.ChatThread
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun listenThreadsByProject(projectId: String): Flow<List<ChatThread>>
    fun listenMessages(threadId: String): Flow<List<ChatMessage>>
    suspend fun createOrGetThread(projectId: String, taskId: String?, participantIds: List<String>): Result<ChatThread>
    suspend fun sendMessage(threadId: String, message: ChatMessage): Result<Unit>
    suspend fun markAsRead(threadId: String, userId: String): Result<Unit>
}
