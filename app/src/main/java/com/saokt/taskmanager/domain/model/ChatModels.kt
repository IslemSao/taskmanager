package com.saokt.taskmanager.domain.model

import java.util.Date
import java.util.UUID

data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val taskId: String?,
    val participantIds: List<String>,
    val createdAt: Date = Date(),
    val lastMessagePreview: String? = null,
    val lastUpdatedAt: Date = Date()
)

enum class ChatMessageType { TEXT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val senderId: String,
    val text: String,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val timestamp: Date = Date(),
    val readBy: List<String> = emptyList()
)
