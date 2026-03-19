package com.saokt.taskmanager.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.NotificationRecord
import com.saokt.taskmanager.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState(isLoading = true))
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.getNotifications().collect { notifications ->
                _state.update {
                    it.copy(
                        notifications = notifications,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        notificationRepository.markAsRead(notificationId)
    }

    fun markAllAsRead() {
        notificationRepository.markAllAsRead()
    }

    fun clearAll() {
        notificationRepository.clearAll()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class NotificationsState(
    val notifications: List<NotificationRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
