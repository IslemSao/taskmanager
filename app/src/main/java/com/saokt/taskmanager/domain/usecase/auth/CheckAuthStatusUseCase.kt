package com.saokt.taskmanager.domain.usecase.auth

import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class CheckAuthStatusUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Boolean {
        return userRepository.isUserAuthenticated()
    }
} 