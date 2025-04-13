package com.example.taskmanager.domain.usecase.auth

import com.example.taskmanager.domain.model.User
import com.example.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String, displayName: String): Result<User> {
        return userRepository.signUp(email, password, displayName)
    }
} 