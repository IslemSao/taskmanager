package com.saokt.taskmanager.data.remote.dto

import java.util.Date

data class ChatThreadDto(
    val id: String = "",
    val projectId: String = "",
    val taskId: String? = null,
    val participantIds: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val lastMessagePreview: String? = null,
    val lastUpdatedAt: Date = Date()
)

data class ChatMessageDto(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "TEXT",
    val timestamp: Date = Date(),
    val readBy: List<String> = emptyList()
)
