package com.example.taskmanager.data.remote.dto

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)
