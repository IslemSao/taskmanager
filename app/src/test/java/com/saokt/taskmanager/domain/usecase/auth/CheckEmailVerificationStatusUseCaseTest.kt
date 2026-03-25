package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckEmailVerificationStatusUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val useCase = CheckEmailVerificationStatusUseCase(userRepository)

    @Test
    fun `invoke forwards forceRefresh flag to repository`() = runTest {
        coEvery { userRepository.isCurrentUserEmailVerified(true) } returns Result.success(true)

        val result = useCase(forceRefresh = true)

        assertThat(result.getOrNull()).isTrue()
        coVerify(exactly = 1) { userRepository.isCurrentUserEmailVerified(true) }
    }
}
