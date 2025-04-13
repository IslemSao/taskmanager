package com.example.taskmanager.domain.usecase.auth

import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return userRepository.signOut()
    }
} 