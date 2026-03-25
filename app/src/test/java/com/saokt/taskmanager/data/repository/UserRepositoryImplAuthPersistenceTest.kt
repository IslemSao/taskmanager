package com.saokt.taskmanager.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.saokt.taskmanager.data.local.dao.ProjectDao
import com.saokt.taskmanager.data.local.dao.ProjectInvitationDao
import com.saokt.taskmanager.data.local.dao.ProjectMemberDao
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.local.dao.UserDao
import com.saokt.taskmanager.data.local.entity.UserEntity
import com.saokt.taskmanager.data.mapper.TaskMapper
import com.saokt.taskmanager.data.remote.dto.UserDto
import com.saokt.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.saokt.taskmanager.data.remote.firebase.FirebaseUserSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserRepositoryImplAuthPersistenceTest {

    private val userDao = mockk<UserDao>(relaxed = true)
    private val projectDao = mockk<ProjectDao>(relaxed = true)
    private val taskDao = mockk<TaskDao>(relaxed = true)
    private val invitationDao = mockk<ProjectInvitationDao>(relaxed = true)
    private val firebaseAuthSource = mockk<FirebaseAuthSource>()
    private val projectMemberDao = mockk<ProjectMemberDao>(relaxed = true)
    private val firebaseUserSource = mockk<FirebaseUserSource>(relaxed = true)
    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val taskMapper = mockk<TaskMapper>(relaxed = true)

    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        repository = UserRepositoryImpl(
            userDao = userDao,
            projectDao = projectDao,
            taskDao = taskDao,
            invitationDao = invitationDao,
            firebaseAuthSource = firebaseAuthSource,
            projectMemberDao = projectMemberDao,
            firebaseUserSource = firebaseUserSource,
            firebaseAuth = firebaseAuth,
            taskMapper = taskMapper
        )
    }

    @Test
    fun `signIn success stores user locally and returns domain user`() = runTest {
        val userDto = UserDto(
            id = "u1",
            email = "jane@example.com",
            displayName = "Jane Doe",
            photoUrl = "https://example.com/jane.png"
        )
        coEvery {
            firebaseAuthSource.signIn("jane@example.com", "secret123")
        } returns Result.success(userDto)

        val result = repository.signIn("jane@example.com", "secret123")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.id).isEqualTo("u1")
        assertThat(result.getOrNull()?.email).isEqualTo("jane@example.com")
        assertThat(result.getOrNull()?.displayName).isEqualTo("Jane Doe")
        coVerify(exactly = 1) {
            userDao.insertUser(
                UserEntity(
                    id = "u1",
                    email = "jane@example.com",
                    displayName = "Jane Doe",
                    photoUrl = "https://example.com/jane.png"
                )
            )
        }
    }

    @Test
    fun `signIn failure does not write user locally`() = runTest {
        val error = IllegalStateException("Authentication failed")
        coEvery {
            firebaseAuthSource.signIn("jane@example.com", "secret123")
        } returns Result.failure(error)

        val result = repository.signIn("jane@example.com", "secret123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { userDao.insertUser(any()) }
    }

    @Test
    fun `signUp success stores user locally and returns domain user`() = runTest {
        val userDto = UserDto(
            id = "u2",
            email = "new@example.com",
            displayName = "New User",
            photoUrl = null
        )
        coEvery {
            firebaseAuthSource.signUp("new@example.com", "secret123", "New User")
        } returns Result.success(userDto)

        val result = repository.signUp("new@example.com", "secret123", "New User")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.id).isEqualTo("u2")
        assertThat(result.getOrNull()?.email).isEqualTo("new@example.com")
        coVerify(exactly = 1) {
            userDao.insertUser(
                UserEntity(
                    id = "u2",
                    email = "new@example.com",
                    displayName = "New User",
                    photoUrl = null
                )
            )
        }
    }

    @Test
    fun `signUp failure does not write user locally`() = runTest {
        val error = IllegalStateException("Sign up failed")
        coEvery {
            firebaseAuthSource.signUp("new@example.com", "secret123", "New User")
        } returns Result.failure(error)

        val result = repository.signUp("new@example.com", "secret123", "New User")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { userDao.insertUser(any()) }
    }

    @Test
    fun `google signIn success stores user locally and returns domain user`() = runTest {
        val userDto = UserDto(
            id = "u3",
            email = "google@example.com",
            displayName = "Google User",
            photoUrl = "https://example.com/google.png"
        )
        coEvery {
            firebaseAuthSource.signInWithGoogle("token-123")
        } returns Result.success(userDto)

        val result = repository.signInWithGoogle("token-123")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.id).isEqualTo("u3")
        assertThat(result.getOrNull()?.email).isEqualTo("google@example.com")
        coVerify(exactly = 1) {
            userDao.insertUser(
                UserEntity(
                    id = "u3",
                    email = "google@example.com",
                    displayName = "Google User",
                    photoUrl = "https://example.com/google.png"
                )
            )
        }
    }

    @Test
    fun `google signIn failure does not write user locally`() = runTest {
        val error = IllegalStateException("Google auth failed")
        coEvery {
            firebaseAuthSource.signInWithGoogle("token-123")
        } returns Result.failure(error)

        val result = repository.signInWithGoogle("token-123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { userDao.insertUser(any()) }
    }
}
