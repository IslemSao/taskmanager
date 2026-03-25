package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val deleteAccountUseCase = DeleteAccountUseCase(userRepository)

    @Test
    fun `invoke delegates to repository and returns success`() = runTest {
        coEvery { userRepository.deleteAccount() } returns Result.success(Unit)

        val result = deleteAccountUseCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { userRepository.deleteAccount() }
    }

    @Test
    fun `invoke returns repository failure unchanged`() = runTest {
        val error = IllegalStateException("Delete failed")
        coEvery { userRepository.deleteAccount() } returns Result.failure(error)

        val result = deleteAccountUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 1) { userRepository.deleteAccount() }
    }
}
