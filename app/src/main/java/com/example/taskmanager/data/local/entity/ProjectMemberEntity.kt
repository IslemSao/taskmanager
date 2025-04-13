// data/local/entity/ProjectMemberEntity.kt
package com.example.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "project_members",
    primaryKeys = ["projectId", "userId"] , // This is crucial!
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index("projectId"),
    ]
)
data class ProjectMemberEntity(
    val projectId: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String
)
