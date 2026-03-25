package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SendPasswordResetEmailUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val useCase = SendPasswordResetEmailUseCase(userRepository)

    @Test
    fun `invoke forwards email to repository`() = runTest {
        coEvery { userRepository.sendPasswordResetEmail("jane@example.com") } returns Result.success(Unit)

        val result = useCase("jane@example.com")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { userRepository.sendPasswordResetEmail("jane@example.com") }
    }
}
