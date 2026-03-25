package com.saokt.taskmanager.domain.usecase.auth

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignInWithGoogleUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val signInWithGoogleUseCase = SignInWithGoogleUseCase(userRepository)

    @Test
    fun `invoke forwards token to repository and returns success`() = runTest {
        val user = User(id = "u1", email = "jane@example.com", displayName = "Jane")
        coEvery {
            userRepository.signInWithGoogle("token-123")
        } returns Result.success(user)

        val result = signInWithGoogleUseCase("token-123")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(user)
        coVerify(exactly = 1) {
            userRepository.signInWithGoogle("token-123")
        }
    }

    @Test
    fun `invoke returns repository failure unchanged`() = runTest {
        val error = IllegalStateException("Google auth failed")
        coEvery {
            userRepository.signInWithGoogle(any())
        } returns Result.failure(error)

        val result = signInWithGoogleUseCase("token-123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 1) {
            userRepository.signInWithGoogle("token-123")
        }
    }
}
