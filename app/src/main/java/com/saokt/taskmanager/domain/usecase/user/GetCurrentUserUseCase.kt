package com.saokt.taskmanager.domain.usecase.user

import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<User?> {
        return userRepository.getCurrentUser()
    }
} 