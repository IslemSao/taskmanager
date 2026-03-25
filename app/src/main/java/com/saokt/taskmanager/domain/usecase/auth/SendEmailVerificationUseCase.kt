package com.saokt.taskmanager.domain.usecase.auth

import com.saokt.taskmanager.domain.repository.UserRepository

class SendEmailVerificationUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return userRepository.sendEmailVerification()
    }
}
