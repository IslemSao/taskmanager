package com.saokt.taskmanager.presentation.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.*
import com.saokt.taskmanager.domain.usecase.notification.ManageNotificationScheduleUseCase
import com.saokt.taskmanager.notification.NotificationSettingsManager
import com.saokt.taskmanager.notification.NotificationTypeHandler
import com.saokt.taskmanager.notification.TaskNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsManager: NotificationSettingsManager,
    private val manageNotificationUseCase: ManageNotificationScheduleUseCase,
    private val notificationScheduler: TaskNotificationScheduler,
    private val notificationTypeHandler: NotificationTypeHandler
) : ViewModel() {

    private val _state = MutableStateFlow(EnhancedNotificationSettingsState())
    val state: StateFlow<EnhancedNotificationSettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val notificationPreferences = settingsManager.getNotificationPreferences()
        val globalSettings = settingsManager.getGlobalSettings()
        val activeProfile = settingsManager.getActiveProfile()

        Log.d("NotificationSettingsViewModel", "Loading settings - quietHoursStart: ${globalSettings.quietHoursStart}, quietHoursEnd: ${globalSettings.quietHoursEnd}")

        _state.update {
            it.copy(
                notificationPreferences = notificationPreferences,
                globalSettings = globalSettings,
                activeProfile = activeProfile,
                availableNotificationTypes = NotificationType.values().toList(),
                isLoading = false
            )
        }
    }

    // ========== NOTIFICATION TYPE MANAGEMENT ==========

    fun toggleNotificationType(type: NotificationType, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentPreference = settingsManager.getNotificationPreference(type)
                val updatedPreference = currentPreference.copy(enabled = enabled)
                settingsManager.updateNotificationPreference(updatedPreference)

                // Reload settings to update state
                loadSettings()

                _state.update {
                    it.copy(message = "${getNotificationTypeDisplayName(type)} ${if (enabled) "enabled" else "disabled"}")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to update notification settings: ${e.message}")
                }
            }
        }
    }

    fun updateNotificationSchedule(type: NotificationType, scheduleConfig: NotificationScheduleConfig) {
        viewModelScope.launch {
            try {
                val currentPreference = settingsManager.getNotificationPreference(type)
                val updatedPreference = currentPreference.copy(scheduleConfig = scheduleConfig)
                settingsManager.updateNotificationPreference(updatedPreference)

                // Reload settings to update state
                loadSettings()

                _state.update {
                    it.copy(message = "${getNotificationTypeDisplayName(type)} schedule updated")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to update schedule: ${e.message}")
                }
            }
        }
    }

    fun updateNotificationPriority(type: NotificationType, priority: NotificationPriority) {
        viewModelScope.launch {
            try {
                val currentPreference = settingsManager.getNotificationPreference(type)
                val updatedPreference = currentPreference.copy(priority = priority)
                settingsManager.updateNotificationPreference(updatedPreference)

                // Reload settings to update state
                loadSettings()

                _state.update {
                    it.copy(message = "${getNotificationTypeDisplayName(type)} priority set to ${priority.name}")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to update priority: ${e.message}")
                }
            }
        }
    }

    // ========== GLOBAL SETTINGS MANAGEMENT ==========

    private fun updateGlobalSettings(settings: GlobalNotificationSettings) {
        viewModelScope.launch {
            try {
                settingsManager.saveGlobalSettings(settings)
                // Don't reload settings here since we already updated the UI state
                _state.update {
                    it.copy(message = "Global settings updated")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to update global settings: ${e.message}")
                }
            }
        }
    }

    fun toggleMasterNotifications(enabled: Boolean) {
        // Update UI state immediately
        _state.update {
            it.copy(
                globalSettings = it.globalSettings.copy(masterNotificationsEnabled = enabled)
            )
        }

        // Save to settings manager in background
        val updatedSettings = _state.value.globalSettings.copy(masterNotificationsEnabled = enabled)
        updateGlobalSettings(updatedSettings)
    }

    fun toggleQuietHours(enabled: Boolean) {
        // Update UI state immediately
        _state.update {
            it.copy(
                globalSettings = it.globalSettings.copy(quietHoursEnabled = enabled)
            )
        }

        // Save to settings manager in background
        val updatedSettings = _state.value.globalSettings.copy(quietHoursEnabled = enabled)
        updateGlobalSettings(updatedSettings)
    }

    fun updateQuietHoursStart(startTime: LocalTime) {
        // Update UI state immediately for instant visual feedback
        _state.update {
            it.copy(
                globalSettings = it.globalSettings.copy(
                    quietHoursStart = startTime
                )
            )
        }

        // Save to settings manager in background
        val updatedSettings = _state.value.globalSettings.copy(
            quietHoursStart = startTime
        )
        updateGlobalSettings(updatedSettings)
    }

    fun updateQuietHoursEnd(endTime: LocalTime) {
        // Update UI state immediately for instant visual feedback
        _state.update {
            it.copy(
                globalSettings = it.globalSettings.copy(
                    quietHoursEnd = endTime
                )
            )
        }

        // Save to settings manager in background
        val updatedSettings = _state.value.globalSettings.copy(
            quietHoursEnd = endTime
        )
        updateGlobalSettings(updatedSettings)
    }

    fun updateQuietHours(startTime: LocalTime, endTime: LocalTime) {
        // Update UI state immediately for instant visual feedback
        _state.update {
            it.copy(
                globalSettings = it.globalSettings.copy(
                    quietHoursStart = startTime,
                    quietHoursEnd = endTime
                )
            )
        }

        // Save to settings manager in background
        val updatedSettings = _state.value.globalSettings.copy(
            quietHoursStart = startTime,
            quietHoursEnd = endTime
        )
        updateGlobalSettings(updatedSettings)
    }

    // ========== PROFILE MANAGEMENT ==========

    fun createNotificationProfile(name: String, basePreferences: List<NotificationPreference> = emptyList()) {
        viewModelScope.launch {
            try {
                val profile = NotificationProfile(
                    userId = "current_user", // TODO: Get from auth
                    name = name,
                    preferences = basePreferences.ifEmpty { settingsManager.getNotificationPreferences() }
                )

                val profiles = settingsManager.getNotificationProfiles().toMutableList()
                profiles.add(profile)
                // Note: We'd need to add a saveProfiles method to the manager

                loadSettings()
                _state.update {
                    it.copy(message = "Profile '$name' created")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to create profile: ${e.message}")
                }
            }
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            try {
                settingsManager.setActiveProfile(profileId)
                loadSettings()
                _state.update {
                    it.copy(message = "Switched to profile")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to switch profile: ${e.message}")
                }
            }
        }
    }

    // ========== TESTING AND UTILITIES ==========

    fun testNotification(type: NotificationType) {
        viewModelScope.launch {
            try {
                notificationTypeHandler.handleNotificationType(type)
                _state.update {
                    it.copy(message = "Test ${getNotificationTypeDisplayName(type)} sent!")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to send test notification: ${e.message}")
                }
            }
        }
    }

    fun testPersistence() {
        viewModelScope.launch {
            try {
                // Save current settings
                val currentSettings = _state.value.globalSettings
                settingsManager.saveGlobalSettings(currentSettings)

                // Wait a moment
                kotlinx.coroutines.delay(500)

                // Load settings again
                val loadedSettings = settingsManager.getGlobalSettings()

                // Check if they match
                val persistenceWorks = currentSettings.quietHoursStart == loadedSettings.quietHoursStart &&
                                     currentSettings.quietHoursEnd == loadedSettings.quietHoursEnd

                _state.update {
                    it.copy(
                        message = if (persistenceWorks)
                            "✅ Persistence working! Start: ${currentSettings.quietHoursStart}, End: ${currentSettings.quietHoursEnd}"
                        else
                            "❌ Persistence failed! Saved: ${currentSettings.quietHoursStart}-${currentSettings.quietHoursEnd}, Loaded: ${loadedSettings.quietHoursStart}-${loadedSettings.quietHoursEnd}"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "❌ Persistence test failed: ${e.message}")
                }
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsManager.resetToDefaults()
                loadSettings()
                _state.update {
                    it.copy(message = "Settings reset to defaults")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to reset settings: ${e.message}")
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /**
     * Test quiet hours functionality
     */
    fun testQuietHours() {
        viewModelScope.launch {
            try {
                val currentTime = java.time.LocalTime.now()
                val isInQuietHours = settingsManager.isInQuietHours()
                val globalSettings = settingsManager.getGlobalSettings()
                
                _state.update {
                    it.copy(
                        message = "Quiet Hours Test:\n" +
                                "Current time: ${currentTime}\n" +
                                "Quiet hours enabled: ${globalSettings.quietHoursEnabled}\n" +
                                "Start: ${globalSettings.quietHoursStart}\n" +
                                "End: ${globalSettings.quietHoursEnd}\n" +
                                "Currently in quiet hours: $isInQuietHours"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(message = "Failed to test quiet hours: ${e.message}")
                }
            }
        }
    }

    // ========== HELPER METHODS ==========

    private fun getNotificationTypeDisplayName(type: NotificationType): String {
        return when (type) {
            NotificationType.TASK_REMINDER -> "Task Reminders"
            NotificationType.DUE_SOON_ALERT -> "Due Soon Alerts"
            NotificationType.OVERDUE_ALERT -> "Overdue Alerts"
            NotificationType.HIGH_PRIORITY_REMINDER -> "High Priority Reminders"
            NotificationType.PROJECT_UPDATE -> "Project Updates"
            NotificationType.ASSIGNMENT_UPDATE -> "Assignment Updates"
            NotificationType.COMPLETION_CELEBRATION -> "Completion Celebrations"
            NotificationType.STREAK_REMINDER -> "Streak Reminders"
            NotificationType.DEADLINE_WARNING -> "Deadline Warnings"
            NotificationType.WEEKLY_SUMMARY -> "Weekly Summaries"
            NotificationType.MORNING_BRIEFING -> "Morning Briefings"
            NotificationType.EVENING_REVIEW -> "Evening Reviews"
            NotificationType.CHAT_MESSAGE -> "Chat Messages"
        }
    }

    // ========== BACKWARD COMPATIBILITY METHODS ==========

    fun toggleReminders(enabled: Boolean) {
        toggleNotificationType(NotificationType.TASK_REMINDER, enabled)
    }

    fun toggleDueSoonNotifications(enabled: Boolean) {
        toggleNotificationType(NotificationType.DUE_SOON_ALERT, enabled)
    }

    fun toggleSound(enabled: Boolean) {
        val currentSettings = _state.value.globalSettings
        val updatedSettings = currentSettings.copy(playNotificationSound = enabled)
        updateGlobalSettings(updatedSettings)
    }

    fun toggleVibration(enabled: Boolean) {
        // This would be handled per notification type, but for backward compatibility
        // we can update all preferences
        val preferences = _state.value.notificationPreferences.map {
            it.copy(vibrationEnabled = enabled)
        }
        preferences.forEach { settingsManager.updateNotificationPreference(it) }
        loadSettings()
    }
}

data class EnhancedNotificationSettingsState(
    val notificationPreferences: List<NotificationPreference> = emptyList(),
    val globalSettings: GlobalNotificationSettings = GlobalNotificationSettings(),
    val activeProfile: NotificationProfile? = null,
    val availableNotificationTypes: List<NotificationType> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

// Legacy state for backward compatibility
data class NotificationSettingsState(
    val remindersEnabled: Boolean = true,
    val dueSoonEnabled: Boolean = true,
    val reminderInterval: Int = 2,
    val dueSoonInterval: Int = 6,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val availableReminderIntervals: List<Pair<Int, String>> = emptyList(),
    val availableDueSoonIntervals: List<Pair<Int, String>> = emptyList(),
    val reminderIntervalText: String = "Every 2 hours",
    val dueSoonIntervalText: String = "Every 6 hours",
    val notificationsScheduled: Boolean = false,
    val message: String? = null
)

