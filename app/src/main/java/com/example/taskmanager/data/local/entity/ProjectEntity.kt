package com.example.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskmanager.domain.model.ProjectRole
import com.example.taskmanager.domain.model.SyncStatus
import java.util.Date

// 2. Update ProjectEntity:
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val color: Int?,
    val startDate: Date?,
    val dueDate: Date?,
    val isCompleted: Boolean,
    val createdAt: Date,
    val modifiedAt: Date,
    val syncStatus: SyncStatus,
    val ownerId: String,
    // Members will be stored in a separate table with a relationship
)




