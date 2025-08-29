
// data/local/entity/ProjectInvitationEntity.kt
package com.saokt.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "project_invitations")
data class ProjectInvitationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectTitle: String,
    val inviterId: String,
    val inviterName: String,
    val inviteeId: String,
    val inviteeEmail: String,
    val status: String,
    val createdAt: Date
)
