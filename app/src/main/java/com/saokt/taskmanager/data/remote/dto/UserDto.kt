package com.saokt.taskmanager.data.remote.dto

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)
