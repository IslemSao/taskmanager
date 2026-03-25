package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignInUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val signInUseCase = SignInUseCase(userRepository)

    @Test
    fun `invoke forwards parameters to repository and returns success`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "Jane")
        coEvery {
            userRepository.signIn("jane@example.com", "secret123")
        } returns Result.success(user)

        val result = signInUseCase("jane@example.com", "secret123")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(user)
        coVerify(exactly = 1) {
            userRepository.signIn("jane@example.com", "secret123")
        }
    }

    @Test
    fun `invoke returns repository failure unchanged`() = runTest {
        val error = IllegalStateException("Authentication failed")
        coEvery {
            userRepository.signIn(any(), any())
        } returns Result.failure(error)

        val result = signInUseCase("jane@example.com", "secret123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 1) {
            userRepository.signIn("jane@example.com", "secret123")
        }
    }
}
