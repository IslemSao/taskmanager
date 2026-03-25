package com.saokt.taskmanager.domain.usecase.auth

import com.saokt.taskmanager.domain.repository.UserRepository

class CheckEmailVerificationStatusUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Boolean> {
        return userRepository.isCurrentUserEmailVerified(forceRefresh)
    }
}
