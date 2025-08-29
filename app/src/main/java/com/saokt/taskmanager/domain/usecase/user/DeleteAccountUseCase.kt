package com.saokt.taskmanager.domain.usecase.user

import com.saokt.taskmanager.domain.repository.UserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return userRepository.deleteAccount()
    }
}
