package com.saokt.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Subtask
import com.saokt.taskmanager.domain.model.SyncStatus
import java.util.Date

@Entity(
    tableName = "tasks",
    indices = [Index("projectId")],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Date?,
    val completed: Boolean,
    val priority: Priority,
    val projectId: String?,
    val labels: List<String>,
    val subtasks: List<Subtask>,
    val createdAt: Date,
    val modifiedAt: Date,
    val syncStatus: SyncStatus,
    val userId: String? = null, // Optional userId for multi-user support
    val createdBy: String = "",
    val assignedTo: String? = null,
    val assignedBy: String? = null
)
