package com.saokt.taskmanager.domain.usecase.auth

import com.saokt.taskmanager.domain.repository.UserRepository

class SendPasswordResetEmailUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return userRepository.sendPasswordResetEmail(email)
    }
}
