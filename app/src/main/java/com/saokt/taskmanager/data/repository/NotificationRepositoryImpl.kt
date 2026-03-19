package com.saokt.taskmanager.data.repository

import com.saokt.taskmanager.domain.model.NotificationRecord
import com.saokt.taskmanager.domain.repository.NotificationRepository
import com.saokt.taskmanager.notification.NotificationSettingsManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val settingsManager: NotificationSettingsManager
) : NotificationRepository {

    private val notificationsState = MutableStateFlow(
        settingsManager.getNotificationRecords().sortedByDescending { it.createdAt }
    )

    override fun getNotifications(): Flow<List<NotificationRecord>> = notificationsState.asStateFlow()

    override fun addNotification(record: NotificationRecord) {
        notificationsState.update { current ->
            val updated = (listOf(record) + current).distinctBy { it.id }.sortedByDescending { it.createdAt }
            settingsManager.saveNotificationRecords(updated)
            updated
        }
    }

    override fun markAsRead(notificationId: String) {
        notificationsState.update { current ->
            val updated = current.map { record ->
                if (record.id == notificationId) record.copy(isRead = true) else record
            }
            settingsManager.saveNotificationRecords(updated)
            updated
        }
    }

    override fun markAllAsRead() {
        notificationsState.update { current ->
            val updated = current.map { it.copy(isRead = true) }
            settingsManager.saveNotificationRecords(updated)
            updated
        }
    }

    override fun clearAll() {
        notificationsState.value = emptyList()
        settingsManager.saveNotificationRecords(emptyList())
    }
}
