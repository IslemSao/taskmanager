package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignUpUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val signUpUseCase = SignUpUseCase(userRepository)

    @Test
    fun `invoke forwards parameters to repository and returns success`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "Jane")
        coEvery {
            userRepository.signUp("jane@example.com", "secret123", "Jane")
        } returns Result.success(user)

        val result = signUpUseCase(
            email = "jane@example.com",
            password = "secret123",
            displayName = "Jane"
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(user)
        coVerify(exactly = 1) {
            userRepository.signUp("jane@example.com", "secret123", "Jane")
        }
    }

    @Test
    fun `invoke returns repository failure unchanged`() = runTest {
        val error = IllegalStateException("Sign up failed")
        coEvery {
            userRepository.signUp(any(), any(), any())
        } returns Result.failure(error)

        val result = signUpUseCase(
            email = "jane@example.com",
            password = "secret123",
            displayName = "Jane"
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 1) {
            userRepository.signUp("jane@example.com", "secret123", "Jane")
        }
    }
}
