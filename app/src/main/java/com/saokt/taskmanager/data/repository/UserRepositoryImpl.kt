package com.saokt.taskmanager.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.Transaction
import com.saokt.taskmanager.data.local.dao.ProjectDao
import com.saokt.taskmanager.data.local.dao.ProjectInvitationDao
import com.saokt.taskmanager.data.local.dao.ProjectMemberDao
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.local.dao.UserDao
import com.saokt.taskmanager.data.local.entity.ProjectEntity
import com.saokt.taskmanager.data.local.entity.ProjectInvitationEntity
import com.saokt.taskmanager.data.local.entity.ProjectMemberEntity
import com.saokt.taskmanager.data.local.entity.UserEntity
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.saokt.taskmanager.data.remote.firebase.FirebaseUserSource
import com.saokt.taskmanager.domain.model.SyncStatus
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val invitationDao: ProjectInvitationDao,
    private val firebaseAuthSource: FirebaseAuthSource,
    private val projectMemberDao: ProjectMemberDao,
    private val firebaseUserSource: FirebaseUserSource,
    private val firebaseAuth: FirebaseAuth, // Example: Assuming direct injection for user ID
    private val taskMapper: com.saokt.taskmanager.data.mapper.TaskMapper
): UserRepository {
    companion object {
        private const val TAG = "UserRepository"
    }

    private val taskRetryQueue = ConcurrentLinkedQueue<TaskDto>()
    private val retryMutex = Mutex()

    override suspend fun signIn(email: String, password: String): Result<User> {
        val result = firebaseAuthSource.signIn(email, password)
        return result.mapCatching { userDto ->
            userDao.insertUser(UserEntity(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl))
            User(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            // Update in local database
            val userEntity = UserEntity(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl
            )
            userDao.insertUser(userEntity)

            Log.d(TAG, "User updated successfully: ${user.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(e)
        }
    }


    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        val result = firebaseAuthSource.signInWithGoogle(idToken)
        return result.mapCatching { userDto ->
            userDao.insertUser(UserEntity(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl))
            User(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl)
        }
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        val result = firebaseAuthSource.signUp(email, password, displayName)
        return result.mapCatching { userDto ->
            userDao.insertUser(UserEntity(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl))
            User(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return firebaseAuthSource.sendPasswordResetEmail(email)
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return firebaseAuthSource.sendEmailVerification()
    }

    override suspend fun isCurrentUserEmailVerified(forceRefresh: Boolean): Result<Boolean> {
        return firebaseAuthSource.isCurrentUserEmailVerified(forceRefresh)
    }



    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { entity ->
            entity?.let { User(it.id, it.email, it.displayName, it.photoUrl) }
        }
    }

    override fun isUserAuthenticated(): Boolean {
        return firebaseAuthSource.getCurrentUser() != null
    }

    override suspend fun signOut(): Result<Unit> {
        val result = firebaseAuthSource.signOut()
        if (result.isSuccess) {
            userDao.deleteAllUsers()
            projectDao.deleteAllProjects()
            taskDao.deleteAllTasks()
            invitationDao.deleteAllInvitations()
            projectMemberDao.deleteAllMembers()

            taskRetryQueue.clear()
            safeLogDebug("Cleared local data on sign out")
        }
        return result
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val result = firebaseAuthSource.deleteAccount()
            if (result.isSuccess) {
                userDao.deleteAllUsers()
                projectDao.deleteAllProjects()
                taskDao.deleteAllTasks()
                invitationDao.deleteAllInvitations()
                projectMemberDao.deleteAllMembers()

                taskRetryQueue.clear()
                safeLogDebug("Cleared all local data after account deletion")
            }
            result
        } catch (e: Exception) {
            safeLogError("Failed to delete account", e)
            Result.failure(e)
        }
    }

    private fun safeLogDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun safeLogError(message: String, throwable: Throwable) {
        runCatching { Log.e(TAG, message, throwable) }
    }

    override fun listenToRemoteProjects(): Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>> {
        return firebaseUserSource.listenToUserProjects()
    }

    override fun listenToRemoteTasks(): Flow<Result<List<TaskDto>>> {
        return firebaseUserSource.listenToUserTasks()
    }

    override fun listenToRemoteInvitations(): Flow<Result<List<ProjectInvitationDto>>> {
        return firebaseUserSource.listenToInvitations()
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500,
        operation: suspend () -> T
    ): T {
        var attempt = 0
        var currentDelay = initialDelay
        while (true) { // Loop indefinitely until success or max attempts reached
            try {
                return operation()
            } catch (e: SQLiteConstraintException) {
                attempt++
                if (attempt >= maxAttempts || !e.message.orEmpty().contains("FOREIGN KEY constraint failed", ignoreCase = true)) {
                    Log.e(TAG, "Constraint error during retryable operation", e)
                    throw e
                }
                Log.w(TAG, "Foreign key constraint failed; retrying in ${currentDelay}ms", e)
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(5000)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected retryable operation failure", e)
                throw e
            }
        }
    }

    @Transaction
    override suspend fun syncRemoteMembersToLocal(memberDtos: List<ProjectMemberDto>) {
        Log.d(TAG, "Syncing ${memberDtos.size} remote project members to local DB")
        if (memberDtos.isEmpty()) {
            Log.d(TAG, "No members to sync.")
        }

        try {
            withRetry {
                val memberEntities = memberDtos.map { dto ->
                    ProjectMemberEntity(
                        projectId = dto.projectId,
                        userId = dto.userId,
                        email = dto.email,
                        displayName = dto.displayName,
                        role = dto.role
                    )
                }

                val projectIdsWithMembers = memberDtos.map { it.projectId }.distinct().toSet()

                val currentLocalMembers = if (projectIdsWithMembers.isNotEmpty()) {
                    projectMemberDao.getMembersForProjectsList(projectIdsWithMembers.toList())
                } else {
                    emptyList()
                }


                val membersToDelete = currentLocalMembers.filter { localMember ->
                    memberEntities.none { remoteEntity ->
                        remoteEntity.projectId == localMember.projectId && remoteEntity.userId == localMember.userId
                    }
                }

                if (membersToDelete.isNotEmpty()) {
                    Log.d(TAG, "Deleting ${membersToDelete.size} local project members for projects: $projectIdsWithMembers")
                    projectMemberDao.deleteMembers(membersToDelete)
                }

                val existingProjectIds = projectDao.getAllLocalProjectIds().toSet()
                val (validMemberEntities, skippedMembers) = memberEntities.partition {
                    existingProjectIds.contains(it.projectId)
                }

                if (skippedMembers.isNotEmpty()) {
                    Log.w(TAG, "Skipping ${skippedMembers.size} members due to missing projects")
                }

                if (validMemberEntities.isNotEmpty()) {
                    Log.d(TAG, "Upserting ${validMemberEntities.size} project members locally for projects: $projectIdsWithMembers")
                    projectMemberDao.upsertAll(validMemberEntities)
                } else {
                    Log.d(TAG, "No valid project members to upsert locally for projects: $projectIdsWithMembers")
                }
            }
            Log.d(TAG, "Project member sync transaction complete")

        } catch (e: Exception) {
            Log.e(TAG, "Error during project member sync transaction", e)
            throw e
        }
    }


    @Transaction
    override suspend fun syncRemoteInvitationsToLocal(projectInvitationDtos: List<ProjectInvitationDto>) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            Log.e(TAG, "Cannot sync invitations, user not authenticated.")
            return
        }
        Log.d(TAG, "Syncing ${projectInvitationDtos.size} remote invitations for user $currentUserId to local DB")

        try {
            val invitationEntities = projectInvitationDtos.map { dto ->
                ProjectInvitationEntity(
                    id = dto.id,
                    projectId = dto.projectId,
                    projectTitle = dto.projectTitle,
                    inviterId = dto.inviterId,
                    inviterName = dto.inviterName,
                    inviteeId = dto.inviteeId,
                    inviteeEmail = dto.inviteeEmail,
                    status = dto.status,
                    createdAt = dto.createdAt
                )
            }

            val remoteInvitationIds = projectInvitationDtos.map { it.id }.toSet()
            val localInvitationIds = invitationDao.getAllLocalInvitationIds().toSet()
            val idsToDelete = localInvitationIds - remoteInvitationIds

            withRetry {
                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "Deleting ${idsToDelete.size} local invitations for user $currentUserId")
                    invitationDao.deleteInvitationsByIds(idsToDelete.toList())
                }
                if (invitationEntities.isNotEmpty()) {
                    Log.d(TAG, "Upserting ${invitationEntities.size} invitations locally for user $currentUserId")
                    invitationDao.upsertAllInvitations(invitationEntities)
                } else {
                    Log.d(TAG, "No invitations to upsert locally for user $currentUserId")
                }
            }
            Log.d(TAG, "Invitation sync transaction complete for user $currentUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error during invitation sync transaction for user $currentUserId", e)
            throw e
        }
    }


    @Transaction
    override suspend fun syncRemoteProjectsToLocal(projectDtos: List<ProjectDto>) {
        Log.d(TAG, "Syncing ${projectDtos.size} remote projects to local DB")
        var projectsUpserted = false
        try {
            val projectEntities = projectDtos.map { dto ->
                ProjectEntity(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    color = dto.color,
                    startDate = dto.startDate,
                    dueDate = dto.dueDate,
                    isCompleted = dto.completed,
                    createdAt = dto.createdAt,
                    modifiedAt = dto.modifiedAt,
                    syncStatus = SyncStatus.SYNCED,
                    ownerId = dto.ownerId
                )
            }

            val remoteProjectIds = projectDtos.map { it.id }.toSet()
            val localProjectIds = projectDao.getAllLocalProjectIds().toSet()
            val idsToDelete = localProjectIds - remoteProjectIds

            withRetry {
                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "Deleting ${idsToDelete.size} projects locally")
                    projectDao.deleteByIds(idsToDelete.toList())
                }

                if (projectEntities.isNotEmpty()) {
                    Log.d(TAG, "Upserting ${projectEntities.size} projects locally")
                    projectDao.upsertAll(projectEntities)
                    projectsUpserted = true
                } else {
                    Log.d(TAG, "No projects to upsert locally.")
                }
            }

            Log.d(TAG, "Project sync transaction complete.")

            if (projectsUpserted || !taskRetryQueue.isEmpty()) {
                Log.d(TAG, "Project sync successful, retrying pending tasks if needed")
                retryPendingTasks()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during project sync transaction", e)
            throw e
        }
    }


    @Transaction
    override suspend fun syncRemoteTasksToLocal(taskDtos: List<TaskDto>) {
        Log.d(TAG, "Syncing ${taskDtos.size} remote tasks to local DB")

        try {
            val existingProjectIds = projectDao.getAllLocalProjectIds().toSet()
            val validTaskDtos = mutableListOf<TaskDto>()
            val newlyPendingTasks = mutableListOf<TaskDto>()

            for (dto in taskDtos) {
                if (dto.projectId == null || existingProjectIds.contains(dto.projectId)) {
                    validTaskDtos.add(dto)
                } else {
                    newlyPendingTasks.add(dto)
                }
            }

            if (newlyPendingTasks.isNotEmpty()) {
                retryMutex.withLock {
                    newlyPendingTasks.forEach { dto ->
                        if (!taskRetryQueue.contains(dto)) {
                            taskRetryQueue.offer(dto)
                        }
                    }
                    Log.d(TAG, "Task retry queue size: ${taskRetryQueue.size}")
                }
            }

            val taskEntities = validTaskDtos.map { dto ->
                val task = taskMapper.dtoToDomain(dto)
                taskMapper.domainToEntity(task)
            }

            val remoteTaskIds = taskDtos.map { it.id }.toSet()
            val localTaskIds = taskDao.getAllLocalTaskIds().toSet()
            val idsToDelete = localTaskIds - remoteTaskIds

            withRetry {
                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "Deleting ${idsToDelete.size} local tasks")
                    taskDao.deleteByIds(idsToDelete.toList())
                }

                if (taskEntities.isNotEmpty()) {
                    Log.d(TAG, "Upserting ${taskEntities.size} valid tasks locally")
                    taskDao.upsertAll(taskEntities)
                } else if (taskDtos.isNotEmpty() && validTaskDtos.isEmpty()) {
                    Log.d(TAG, "No currently valid tasks to upsert; all are waiting on projects")
                } else {
                    Log.d(TAG, "No tasks to upsert locally")
                }
            }

            Log.d(TAG, "Task sync transaction complete")

        } catch (e: Exception) {
            Log.e(TAG, "Error during task sync transaction", e)
            throw e
        }
    }


    private suspend fun retryPendingTasks() {
        retryMutex.withLock {
            if (taskRetryQueue.isEmpty()) {
                return
            }

            Log.i(TAG, "Attempting to retry ${taskRetryQueue.size} pending tasks")
            val currentlyExistingProjectIds = projectDao.getAllLocalProjectIds().toSet()
            val tasksToRetryNow = mutableListOf<TaskDto>()
            val tasksStillPending = ConcurrentLinkedQueue<TaskDto>()

            while(taskRetryQueue.isNotEmpty()) {
                val taskDto = taskRetryQueue.poll() ?: continue

                if (taskDto.projectId == null || currentlyExistingProjectIds.contains(taskDto.projectId)) {
                    tasksToRetryNow.add(taskDto)
                } else {
                    tasksStillPending.offer(taskDto)
                }
            }

            if (tasksToRetryNow.isNotEmpty()) {
                Log.i(TAG, "Retrying ${tasksToRetryNow.size} tasks whose projects are now available")
                val retryEntities = tasksToRetryNow.map { dto ->
                    taskMapper.domainToEntity(taskMapper.dtoToDomain(dto))
                }
                try {
                    withRetry(maxAttempts = 2, initialDelay = 100) {
                        taskDao.upsertAll(retryEntities)
                    }
                    Log.i(TAG, "Successfully retried and upserted ${retryEntities.size} tasks")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during task retry upsert; adding tasks back to queue", e)
                    tasksToRetryNow.forEach { tasksStillPending.offer(it) }
                }
            }

            taskRetryQueue.addAll(tasksStillPending)

            if (taskRetryQueue.isNotEmpty()) {
                Log.w(TAG, "${taskRetryQueue.size} tasks still pending retry")
            } else {
                Log.i(TAG, "Task retry queue is now empty after attempt")
            }
        }
    }
}
