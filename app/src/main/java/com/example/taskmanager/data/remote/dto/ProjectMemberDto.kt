package com.example.taskmanager.data.remote.dto

import java.util.Date

data class ProjectMemberDto(
    val projectId: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String, // Store as string in Firestore
    val joinedAt: Date = Date() // When the member joined the project
) {
    constructor() : this("", "", "", "", "MEMBER", Date())
}