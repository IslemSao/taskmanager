package com.saokt.taskmanager.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.saokt.taskmanager.domain.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NotificationSettingsManager"
        private const val PREFS_NAME = "enhanced_notification_settings"

        // Legacy keys for backward compatibility
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_DUE_SOON_ENABLED = "due_soon_enabled"
        private const val KEY_REMINDER_INTERVAL = "reminder_interval_hours"
        private const val KEY_DUE_SOON_INTERVAL = "due_soon_interval_hours"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_NOTIFICATION_VIBRATION = "notification_vibration"

        // New enhanced keys
        private const val KEY_NOTIFICATION_PREFERENCES = "notification_preferences"
        private const val KEY_GLOBAL_SETTINGS = "global_settings"
        private const val KEY_NOTIFICATION_PROFILES = "notification_profiles"
        private const val KEY_ACTIVE_PROFILE = "active_profile"
        private const val KEY_NOTIFICATION_HISTORY = "notification_history"
        private const val KEY_SMART_RULES = "smart_rules"

        // Default values
        private const val DEFAULT_REMINDER_INTERVAL = 2
        private const val DEFAULT_DUE_SOON_INTERVAL = 6
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .create()

    // ========== LEGACY METHODS (for backward compatibility) ==========

    var areRemindersEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_REMINDERS_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()

    var areDueSoonNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_DUE_SOON_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_DUE_SOON_ENABLED, value).apply()

    var reminderIntervalHours: Int
        get() = sharedPreferences.getInt(KEY_REMINDER_INTERVAL, DEFAULT_REMINDER_INTERVAL)
        set(value) = sharedPreferences.edit().putInt(KEY_REMINDER_INTERVAL, value).apply()

    var dueSoonIntervalHours: Int
        get() = sharedPreferences.getInt(KEY_DUE_SOON_INTERVAL, DEFAULT_DUE_SOON_INTERVAL)
        set(value) = sharedPreferences.edit().putInt(KEY_DUE_SOON_INTERVAL, value).apply()

    var isNotificationSoundEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATION_SOUND, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, value).apply()

    var isNotificationVibrationEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATION_VIBRATION, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, value).apply()

    // ========== ENHANCED NOTIFICATION SYSTEM ==========

    /**
     * Get all notification preferences
     */
    fun getNotificationPreferences(): List<NotificationPreference> {
        val json = sharedPreferences.getString(KEY_NOTIFICATION_PREFERENCES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<NotificationPreference>>() {}.type
                gson.fromJson(json, type) ?: getDefaultNotificationPreferences()
            } catch (e: Exception) {
                getDefaultNotificationPreferences()
            }
        } else {
            getDefaultNotificationPreferences()
        }
    }

    /**
     * Save notification preferences
     */
    fun saveNotificationPreferences(preferences: List<NotificationPreference>) {
        val json = gson.toJson(preferences)
        sharedPreferences.edit().putString(KEY_NOTIFICATION_PREFERENCES, json).apply()
    }

    /**
     * Get preference for specific notification type
     */
    fun getNotificationPreference(type: NotificationType): NotificationPreference {
        return getNotificationPreferences().find { it.type == type } ?: getDefaultPreferenceForType(type)
    }

    /**
     * Update preference for specific notification type
     */
    fun updateNotificationPreference(preference: NotificationPreference) {
        val preferences = getNotificationPreferences().toMutableList()
        val index = preferences.indexOfFirst { it.type == preference.type }
        if (index >= 0) {
            preferences[index] = preference.copy(modifiedAt = java.util.Date())
        } else {
            preferences.add(preference)
        }
        saveNotificationPreferences(preferences)
    }

    /**
     * Get global notification settings
     */
    fun getGlobalSettings(): GlobalNotificationSettings {
        val json = sharedPreferences.getString(KEY_GLOBAL_SETTINGS, null)
        return if (json != null) {
            try {
                val settings = gson.fromJson(json, GlobalNotificationSettings::class.java) ?: GlobalNotificationSettings()
                Log.d(TAG, "Loaded global settings: quietHoursStart=${settings.quietHoursStart}, quietHoursEnd=${settings.quietHoursEnd}")
                settings
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load global settings, using defaults", e)
                GlobalNotificationSettings()
            }
        } else {
            Log.d(TAG, "No saved global settings found, using defaults")
            GlobalNotificationSettings()
        }
    }

    /**
     * Save global notification settings
     */
    fun saveGlobalSettings(settings: GlobalNotificationSettings) {
        val json = gson.toJson(settings)
        sharedPreferences.edit().putString(KEY_GLOBAL_SETTINGS, json).apply()
        Log.d(TAG, "Saved global settings: quietHoursStart=${settings.quietHoursStart}, quietHoursEnd=${settings.quietHoursEnd}")
    }

    /**
     * Get notification profiles
     */
    fun getNotificationProfiles(): List<NotificationProfile> {
        val json = sharedPreferences.getString(KEY_NOTIFICATION_PROFILES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<NotificationProfile>>() {}.type
                gson.fromJson(json, type) ?: getDefaultProfiles()
            } catch (e: Exception) {
                getDefaultProfiles()
            }
        } else {
            getDefaultProfiles()
        }
    }

    /**
     * Get active notification profile
     */
    fun getActiveProfile(): NotificationProfile? {
        val profiles = getNotificationProfiles()
        val activeProfileId = sharedPreferences.getString(KEY_ACTIVE_PROFILE, null)
        return if (activeProfileId != null) {
            profiles.find { it.id == activeProfileId }
        } else {
            profiles.firstOrNull { it.isActive }
        }
    }

    /**
     * Set active profile
     */
    fun setActiveProfile(profileId: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply()
    }

    /**
     * Get smart notification rules
     */
    fun getSmartRules(): List<SmartNotificationRule> {
        val json = sharedPreferences.getString(KEY_SMART_RULES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<SmartNotificationRule>>() {}.type
                gson.fromJson(json, type) ?: getDefaultSmartRules()
            } catch (e: Exception) {
                getDefaultSmartRules()
            }
        } else {
            getDefaultSmartRules()
        }
    }

    /**
     * Save smart notification rules
     */
    fun saveSmartRules(rules: List<SmartNotificationRule>) {
        val json = gson.toJson(rules)
        sharedPreferences.edit().putString(KEY_SMART_RULES, json).apply()
    }

    // ========== UTILITY METHODS ==========

    /**
     * Get available reminder intervals (legacy support)
     */
    fun getAvailableReminderIntervals(): List<Pair<Int, String>> {
        return listOf(
            1 to "Every hour",
            2 to "Every 2 hours",
            4 to "Every 4 hours",
            6 to "Every 6 hours",
            12 to "Every 12 hours",
            24 to "Daily"
        )
    }

    /**
     * Get available due soon check intervals (legacy support)
     */
    fun getAvailableDueSoonIntervals(): List<Pair<Int, String>> {
        return listOf(
            2 to "Every 2 hours",
            4 to "Every 4 hours",
            6 to "Every 6 hours",
            12 to "Every 12 hours",
            24 to "Daily"
        )
    }

    /**
     * Check if notifications are enabled for a specific type
     */
    fun isNotificationTypeEnabled(type: NotificationType): Boolean {
        val preference = getNotificationPreference(type)
        val globalSettings = getGlobalSettings()
        return preference.enabled && globalSettings.masterNotificationsEnabled
    }

    /**
     * Check if current time is within quiet hours
     */
    fun isInQuietHours(): Boolean {
        val globalSettings = getGlobalSettings()
        if (!globalSettings.quietHoursEnabled) return false

        val now = LocalTime.now()
        val start = globalSettings.quietHoursStart
        val end = globalSettings.quietHoursEnd

        return if (start.isBefore(end)) {
            // Same day quiet hours (e.g., 22:00 to 08:00)
            now.isAfter(start) && now.isBefore(end)
        } else {
            // Overnight quiet hours (e.g., 22:00 to 08:00 next day)
            now.isAfter(start) || now.isBefore(end)
        }
    }

    /**
     * Reset all settings to default
     */
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }

    // ========== PRIVATE HELPER METHODS ==========

    private fun getDefaultNotificationPreferences(): List<NotificationPreference> {
        return NotificationType.values().map { getDefaultPreferenceForType(it) }
    }

    private fun getDefaultPreferenceForType(type: NotificationType): NotificationPreference {
        return when (type) {
            NotificationType.TASK_REMINDER -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.MEDIUM,
                scheduleConfig = NotificationScheduleConfig(intervalHours = 2)
            )
            NotificationType.DUE_SOON_ALERT -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.HIGH,
                scheduleConfig = NotificationScheduleConfig(intervalHours = 6, dueDateThresholdHours = 24)
            )
            NotificationType.OVERDUE_ALERT -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.URGENT,
                scheduleConfig = NotificationScheduleConfig(intervalHours = 1)
            )
            NotificationType.HIGH_PRIORITY_REMINDER -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.HIGH,
                scheduleConfig = NotificationScheduleConfig(intervalHours = 1, priorityThreshold = Priority.HIGH)
            )
            NotificationType.PROJECT_UPDATE -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.MEDIUM,
                trigger = NotificationTrigger.EVENT_BASED
            )
            NotificationType.ASSIGNMENT_UPDATE -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.MEDIUM,
                trigger = NotificationTrigger.EVENT_BASED
            )
            NotificationType.COMPLETION_CELEBRATION -> NotificationPreference(
                type = type,
                enabled = false, // Disabled by default
                priority = NotificationPriority.LOW,
                trigger = NotificationTrigger.EVENT_BASED
            )
            NotificationType.STREAK_REMINDER -> NotificationPreference(
                type = type,
                enabled = false,
                priority = NotificationPriority.MEDIUM,
                scheduleConfig = NotificationScheduleConfig(
                    customTimes = listOf(LocalTime.of(9, 0)),
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                )
            )
            NotificationType.DEADLINE_WARNING -> NotificationPreference(
                type = type,
                enabled = true,
                priority = NotificationPriority.HIGH,
                scheduleConfig = NotificationScheduleConfig(dueDateThresholdHours = 6)
            )
            NotificationType.WEEKLY_SUMMARY -> NotificationPreference(
                type = type,
                enabled = false,
                priority = NotificationPriority.LOW,
                scheduleConfig = NotificationScheduleConfig(
                    customTimes = listOf(LocalTime.of(10, 0)),
                    daysOfWeek = setOf(DayOfWeek.MONDAY)
                )
            )
            NotificationType.MORNING_BRIEFING -> NotificationPreference(
                type = type,
                enabled = false,
                priority = NotificationPriority.MEDIUM,
                scheduleConfig = NotificationScheduleConfig(
                    customTimes = listOf(LocalTime.of(8, 30)),
                    daysOfWeek = DayOfWeek.values().toSet()
                )
            )
            NotificationType.EVENING_REVIEW -> NotificationPreference(
                type = type,
                enabled = false,
                priority = NotificationPriority.MEDIUM,
                scheduleConfig = NotificationScheduleConfig(
                    customTimes = listOf(LocalTime.of(19, 0)),
                    daysOfWeek = DayOfWeek.values().toSet()
                )
            )
        }
    }

    private fun getDefaultProfiles(): List<NotificationProfile> {
        return listOf(
            NotificationProfile(
                id = "default",
                userId = "default",
                name = "Default Profile",
                isActive = true,
                preferences = getDefaultNotificationPreferences()
            )
        )
    }

    private fun getDefaultSmartRules(): List<SmartNotificationRule> {
        return listOf(
            SmartNotificationRule(
                id = "high_priority_due_soon",
                name = "High Priority Due Soon",
                description = "Notify for high priority tasks due within 12 hours",
                enabled = true,
                triggerConditions = listOf(
                    NotificationTriggerCondition(
                        type = NotificationTriggerConditionType.DUE_DATE_WITHIN_HOURS,
                        operator = ConditionOperator.LESS_THAN_OR_EQUAL,
                        value = "12"
                    ),
                    NotificationTriggerCondition(
                        type = NotificationTriggerConditionType.TASK_PRIORITY_ABOVE,
                        operator = ConditionOperator.EQUALS,
                        value = Priority.HIGH.name
                    )
                ),
                actions = listOf(NotificationActionType.SHOW_NOTIFICATION),
                priority = NotificationPriority.URGENT
            ),
            SmartNotificationRule(
                id = "overdue_tasks",
                name = "Overdue Tasks Alert",
                description = "Frequent reminders for overdue tasks",
                enabled = true,
                triggerConditions = listOf(
                    NotificationTriggerCondition(
                        type = NotificationTriggerConditionType.TASK_OVERDUE_BY_HOURS,
                        operator = ConditionOperator.GREATER_THAN,
                        value = "1"
                    )
                ),
                actions = listOf(NotificationActionType.SHOW_NOTIFICATION),
                priority = NotificationPriority.HIGH
            )
        )
    }
}

// Custom adapter for LocalTime serialization/deserialization
private class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun serialize(src: LocalTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src.format(formatter))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalTime {
        return LocalTime.parse(json.asString, formatter)
    }
}
