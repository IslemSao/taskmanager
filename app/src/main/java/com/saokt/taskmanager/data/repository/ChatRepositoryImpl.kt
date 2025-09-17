package com.saokt.taskmanager.data.repository

import com.saokt.taskmanager.data.mapper.ChatMapper
import com.saokt.taskmanager.data.remote.firebase.FirebaseChatSource
import com.saokt.taskmanager.domain.model.ChatMessage
import com.saokt.taskmanager.domain.model.ChatThread
import com.saokt.taskmanager.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl @Inject constructor(
    private val firebaseChatSource: FirebaseChatSource,
    private val mapper: ChatMapper
) : ChatRepository {
    override fun listenThreadsByProject(projectId: String): Flow<List<ChatThread>> =
        firebaseChatSource.listenThreadsByProject(projectId).map { list -> list.map(mapper::dtoToDomain) }

    override fun listenMessages(threadId: String): Flow<List<ChatMessage>> =
        firebaseChatSource.listenMessages(threadId).map { list -> list.map(mapper::dtoToDomain) }

    override suspend fun createOrGetThread(projectId: String, taskId: String?, participantIds: List<String>): Result<ChatThread> {
        return firebaseChatSource.createOrGetThread(projectId, taskId, participantIds).map { mapper.dtoToDomain(it) }
    }

    override suspend fun sendMessage(threadId: String, message: ChatMessage): Result<Unit> {
        return firebaseChatSource.sendMessage(threadId, mapper.domainToDto(message))
    }

    override suspend fun markAsRead(threadId: String, userId: String): Result<Unit> {
        return firebaseChatSource.markAsRead(threadId, userId)
    }
}

// Quick Result.map extension (since Kotlin stdlib lacks it on Result until newer versions)
private inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
    fold(onSuccess = { Result.success(transform(it)) }, onFailure = { Result.failure(it) })
