package com.saokt.taskmanager.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.saokt.taskmanager.data.local.dao.ProjectDao
import com.saokt.taskmanager.data.local.dao.ProjectInvitationDao
import com.saokt.taskmanager.data.local.dao.ProjectMemberDao
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.local.dao.UserDao
import com.saokt.taskmanager.data.mapper.TaskMapper
import com.saokt.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.saokt.taskmanager.data.remote.firebase.FirebaseUserSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserRepositoryImplAuthLifecycleTest {

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
    fun `signOut success clears all local auth scoped data`() = runTest {
        every { firebaseAuthSource.signOut() } returns Result.success(Unit)

        val result = repository.signOut()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { userDao.deleteAllUsers() }
        coVerify(exactly = 1) { projectDao.deleteAllProjects() }
        coVerify(exactly = 1) { taskDao.deleteAllTasks() }
        coVerify(exactly = 1) { invitationDao.deleteAllInvitations() }
        coVerify(exactly = 1) { projectMemberDao.deleteAllMembers() }
    }

    @Test
    fun `signOut failure does not clear local data`() = runTest {
        val error = IllegalStateException("Sign out failed")
        every { firebaseAuthSource.signOut() } returns Result.failure(error)

        val result = repository.signOut()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { userDao.deleteAllUsers() }
        coVerify(exactly = 0) { projectDao.deleteAllProjects() }
        coVerify(exactly = 0) { taskDao.deleteAllTasks() }
        coVerify(exactly = 0) { invitationDao.deleteAllInvitations() }
        coVerify(exactly = 0) { projectMemberDao.deleteAllMembers() }
    }

    @Test
    fun `deleteAccount success clears all local auth scoped data`() = runTest {
        coEvery { firebaseAuthSource.deleteAccount() } returns Result.success(Unit)

        val result = repository.deleteAccount()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { userDao.deleteAllUsers() }
        coVerify(exactly = 1) { projectDao.deleteAllProjects() }
        coVerify(exactly = 1) { taskDao.deleteAllTasks() }
        coVerify(exactly = 1) { invitationDao.deleteAllInvitations() }
        coVerify(exactly = 1) { projectMemberDao.deleteAllMembers() }
    }

    @Test
    fun `deleteAccount failure does not clear local data`() = runTest {
        val error = IllegalStateException("Delete failed")
        coEvery { firebaseAuthSource.deleteAccount() } returns Result.failure(error)

        val result = repository.deleteAccount()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { userDao.deleteAllUsers() }
        coVerify(exactly = 0) { projectDao.deleteAllProjects() }
        coVerify(exactly = 0) { taskDao.deleteAllTasks() }
        coVerify(exactly = 0) { invitationDao.deleteAllInvitations() }
        coVerify(exactly = 0) { projectMemberDao.deleteAllMembers() }
    }

    @Test
    fun `deleteAccount exception is returned and does not clear local data`() = runTest {
        val error = IllegalStateException("Delete exploded")
        coEvery { firebaseAuthSource.deleteAccount() } throws error

        val result = repository.deleteAccount()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { userDao.deleteAllUsers() }
        coVerify(exactly = 0) { projectDao.deleteAllProjects() }
        coVerify(exactly = 0) { taskDao.deleteAllTasks() }
        coVerify(exactly = 0) { invitationDao.deleteAllInvitations() }
        coVerify(exactly = 0) { projectMemberDao.deleteAllMembers() }
    }
}
