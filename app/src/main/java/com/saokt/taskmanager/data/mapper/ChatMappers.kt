package com.saokt.taskmanager.data.mapper

import com.saokt.taskmanager.data.remote.dto.ChatMessageDto
import com.saokt.taskmanager.data.remote.dto.ChatThreadDto
import com.saokt.taskmanager.domain.model.ChatMessage
import com.saokt.taskmanager.domain.model.ChatMessageType
import com.saokt.taskmanager.domain.model.ChatThread

import javax.inject.Inject

class ChatMapper @Inject constructor() {
    fun dtoToDomain(dto: ChatThreadDto): ChatThread = ChatThread(
        id = dto.id,
        projectId = dto.projectId,
        taskId = dto.taskId,
        participantIds = dto.participantIds,
        createdAt = dto.createdAt,
        lastMessagePreview = dto.lastMessagePreview,
        lastUpdatedAt = dto.lastUpdatedAt
    )

    fun domainToDto(thread: ChatThread): ChatThreadDto = ChatThreadDto(
        id = thread.id,
        projectId = thread.projectId,
        taskId = thread.taskId,
        participantIds = thread.participantIds,
        createdAt = thread.createdAt,
        lastMessagePreview = thread.lastMessagePreview,
        lastUpdatedAt = thread.lastUpdatedAt
    )

    fun dtoToDomain(dto: ChatMessageDto): ChatMessage = ChatMessage(
        id = dto.id,
        threadId = dto.threadId,
        senderId = dto.senderId,
        text = dto.text,
        type = ChatMessageType.valueOf(dto.type),
        timestamp = dto.timestamp,
        readBy = dto.readBy
    )

    fun domainToDto(message: ChatMessage): ChatMessageDto = ChatMessageDto(
        id = message.id,
        threadId = message.threadId,
        senderId = message.senderId,
        text = message.text,
        type = message.type.name,
        timestamp = message.timestamp,
        readBy = message.readBy
    )
}
