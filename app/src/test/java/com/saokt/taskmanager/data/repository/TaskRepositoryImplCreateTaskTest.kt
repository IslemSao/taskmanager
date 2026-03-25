package com.saokt.taskmanager.data.repository

import android.content.Context
import android.net.ConnectivityManager
import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.mapper.TaskMapper
import com.saokt.taskmanager.data.remote.firebase.FirebaseTaskSource
import com.saokt.taskmanager.domain.model.SyncStatus
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.widget.WidgetRefresher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TaskRepositoryImplCreateTaskTest {
    private val taskDao = mockk<TaskDao>()
    private val firebaseTaskSource = mockk<FirebaseTaskSource>()
    private val taskMapper = TaskMapper()
    private val userRepository = mockk<UserRepository>()
    private val widgetRefresher = mockk<WidgetRefresher>()
    private val context = mockk<Context>()
    private val connectivityManager = mockk<ConnectivityManager>()

    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null
        coEvery { widgetRefresher.refreshTopTasksWidget() } just runs
        coEvery { taskDao.insertTask(any()) } just runs
        coEvery { taskDao.updateSyncStatus(any(), any()) } just runs

        repository = TaskRepositoryImpl(
            taskDao = taskDao,
            firebaseTaskSource = firebaseTaskSource,
            taskMapper = taskMapper,
            userRepository = userRepository,
            widgetRefresher = widgetRefresher,
            context = context
        )
    }

    @Test
    fun `createTask returns authentication failure when no current user`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(null)

        val result = repository.createTask(Task(title = "Write docs"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("User not authenticated")
        coVerify(exactly = 0) { taskDao.insertTask(any()) }
    }

    @Test
    fun `createTask writes local pending task and skips remote sync while offline`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(User(id = "user-1", email = "user@example.com"))
        val input = Task(title = "Local create", createdBy = "", assignedTo = "user-2")

        val result = repository.createTask(input)

        assertThat(result.isSuccess).isTrue()
        val saved = result.getOrNull()!!
        assertThat(saved.userId).isEqualTo("user-1")
        assertThat(saved.createdBy).isEqualTo("user-1")
        assertThat(saved.syncStatus).isEqualTo(SyncStatus.PENDING)
        assertThat(saved.visibleToUserIds).containsExactly("user-1", "user-2")

        coVerify(exactly = 1) { taskDao.insertTask(match { it.id == saved.id && it.syncStatus == SyncStatus.PENDING }) }
        coVerify(exactly = 0) { firebaseTaskSource.createTask(any()) }
        coVerify(exactly = 1) { widgetRefresher.refreshTopTasksWidget() }
    }

    @Test
    fun `createTask preserves explicit creator when provided and visibility reflects creator plus assignee`() = runTest {
        every { userRepository.getCurrentUser() } returns flowOf(User(id = "session-user", email = "session@example.com"))
        val input = Task(title = "Delegated task", createdBy = "owner-123", assignedTo = "member-456")

        val result = repository.createTask(input)

        assertThat(result.isSuccess).isTrue()
        val saved = result.getOrNull()!!
        assertThat(saved.userId).isEqualTo("session-user")
        assertThat(saved.createdBy).isEqualTo("owner-123")
        assertThat(saved.visibleToUserIds).containsExactly("owner-123", "member-456")
        coVerify(exactly = 1) { taskDao.insertTask(match { it.createdBy == "owner-123" && it.userId == "session-user" }) }
    }
}
