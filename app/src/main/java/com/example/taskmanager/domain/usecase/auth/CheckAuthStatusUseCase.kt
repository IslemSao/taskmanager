package com.example.taskmanager.domain.usecase.auth

import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class CheckAuthStatusUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Boolean {
        return userRepository.isUserAuthenticated()
    }
} 