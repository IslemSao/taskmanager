package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SendEmailVerificationUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val useCase = SendEmailVerificationUseCase(userRepository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        coEvery { userRepository.sendEmailVerification() } returns Result.success(Unit)

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { userRepository.sendEmailVerification() }
    }
}
