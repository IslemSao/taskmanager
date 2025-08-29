package com.saokt.taskmanager.domain.usecase.auth

import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return userRepository.signInWithGoogle(idToken)
    }
} 