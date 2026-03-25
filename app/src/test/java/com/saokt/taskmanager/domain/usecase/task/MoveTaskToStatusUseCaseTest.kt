package com.saokt.taskmanager.domain.usecase.task

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MoveTaskToStatusUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = MoveTaskToStatusUseCase(taskRepository)

    @Test
    fun `moving a task to done synchronizes completed flag`() = runTest {
        coEvery { taskRepository.updateTask(any()) } answers { Result.success(firstArg()) }

        val result = useCase(Task(id = "1", title = "Ship", completed = false), TaskStatus.DONE)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.status).isEqualTo(TaskStatus.DONE)
        assertThat(result.getOrNull()?.completed).isTrue()
        coVerify {
            taskRepository.updateTask(match { it.status == TaskStatus.DONE && it.completed })
        }
    }
}
