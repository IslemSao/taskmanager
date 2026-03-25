package com.saokt.taskmanager.domain.usecase.task

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CreateTaskUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val userRepository = mockk<UserRepository>()
    private lateinit var useCase: CreateTaskUseCase

    @Before
    fun setUp() {
        useCase = CreateTaskUseCase(taskRepository, userRepository)
    }

    @Test
    fun `blank title fails validation`() = runTest {
        val result = useCase(Task(title = "   "))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Task title cannot be empty")
        coVerify(exactly = 0) { taskRepository.createTask(any()) }
    }

    @Test
    fun `unauthenticated user fails task creation`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(null)

        val result = useCase(Task(title = "Write tests"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("User not authenticated")
        coVerify(exactly = 0) { taskRepository.createTask(any()) }
    }

    @Test
    fun `sets createdBy to current user when absent`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(User(id = "u1", email = "u1@example.com"))
        coEvery { taskRepository.createTask(any()) } answers { Result.success(firstArg()) }

        val input = Task(title = "Ship v1", createdBy = "")
        val result = useCase(input)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.createdBy).isEqualTo("u1")
        coVerify(exactly = 1) {
            taskRepository.createTask(match { it.createdBy == "u1" && it.title == "Ship v1" })
        }
    }

    @Test
    fun `preserves explicit createdBy when provided`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(User(id = "u1", email = "u1@example.com"))
        coEvery { taskRepository.createTask(any()) } answers { Result.success(firstArg()) }

        val input = Task(title = "Owner-set task", createdBy = "owner-123")
        val result = useCase(input)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.createdBy).isEqualTo("owner-123")
    }

    @Test
    fun `supports unicode title when authenticated`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(User(id = "u1", email = "u1@example.com"))
        coEvery { taskRepository.createTask(any()) } answers { Result.success(firstArg()) }

        val result = useCase(Task(title = "إنهاء تقرير المشروع"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.title).isEqualTo("إنهاء تقرير المشروع")
    }

    @Test
    fun `repository failure is propagated unchanged`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(User(id = "u1", email = "u1@example.com"))
        coEvery { taskRepository.createTask(any()) } returns Result.failure(IllegalStateException("DB write failed"))

        val result = useCase(Task(title = "Create task"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("DB write failed")
    }
}
