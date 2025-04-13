package com.example.taskmanager.data.remote.dto

data class ProjectMemberDto(
    val projectId: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String // Store as string in Firestore
) {
    constructor() : this("", "", "", "", "MEMBER")
}