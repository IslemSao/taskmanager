package com.example.taskmanager.data.remote.dto

import java.util.Date

// 3. Update ProjectDto:
data class ProjectDto(
    // Add @get:PropertyName for fields where Kotlin name != Firestore name
    // No need for id, title, description, ownerId if names match exactly

    val id: String = "", // Add default values for all properties
    val title: String = "",
    val description: String = "",
    val color: Int? = null,
    val startDate: Date? = null,
    val dueDate: Date? = null,

    val completed: Boolean = false,

    val createdAt: Date = Date(), // Provide sensible defaults
    val modifiedAt: Date = Date(),
    val ownerId: String = "",

    // Change this if 'members' in Firestore is an array of String IDs
    val members: List<String> = emptyList()
    // If members MUST be List<ProjectMemberDto>, see notes below
)

