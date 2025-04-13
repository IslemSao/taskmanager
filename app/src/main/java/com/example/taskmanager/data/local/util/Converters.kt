package com.example.taskmanager.data.local.util

import androidx.room.TypeConverter
import com.example.taskmanager.domain.model.Priority
import com.example.taskmanager.domain.model.SyncStatus
import com.example.taskmanager.domain.model.Subtask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromSubtaskList(value: String): List<Subtask> {
        val listType = object : TypeToken<List<Subtask>>() {}.type
         return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toSubtaskList(list: List<Subtask>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(value: String): Priority {
        return Priority.valueOf(value)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}
