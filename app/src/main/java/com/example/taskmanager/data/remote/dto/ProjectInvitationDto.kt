// data/remote/dto/ProjectInvitationDto.kt
package com.example.taskmanager.data.remote.dto

import java.util.Date

data class ProjectInvitationDto(
    // Use document ID, provide default for no-arg constructor
    val id: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val inviterId: String = "",
    val inviterName: String = "",
    // inviteeId is empty in your example, "" is a valid default
    val inviteeId: String = "",
    val inviteeEmail: String = "",
    val status: String = "",
    // Firestore gives Timestamp, map it to Date.
    // Provide a default for the no-arg constructor requirement.
    val createdAt: Date = Date(0) // Or Date()
)