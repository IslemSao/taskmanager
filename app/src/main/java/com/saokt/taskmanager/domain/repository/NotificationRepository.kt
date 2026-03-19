package com.saokt.taskmanager.domain.repository

import com.saokt.taskmanager.domain.model.NotificationRecord
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(): Flow<List<NotificationRecord>>
    fun addNotification(record: NotificationRecord)
    fun markAsRead(notificationId: String)
    fun markAllAsRead()
    fun clearAll()
}
