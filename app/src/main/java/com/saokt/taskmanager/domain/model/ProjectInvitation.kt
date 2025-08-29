// domain/model/ProjectInvitation.kt
package com.saokt.taskmanager.domain.model

import java.util.Date
import java.util.UUID

data class ProjectInvitation(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectTitle: String,
    val inviterId: String,
    val inviterName: String,
    val inviteeId: String,
    val inviteeEmail: String,
    val status: InvitationStatus = InvitationStatus.PENDING,
    val createdAt: Date = Date()
)

enum class InvitationStatus {
    PENDING, ACCEPTED, REJECTED
}
