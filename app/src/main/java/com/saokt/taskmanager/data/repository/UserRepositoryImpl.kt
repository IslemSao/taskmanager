package com.saokt.taskmanager.data.repository

import android.util.Log
import androidx.room.Transaction // Import Transaction
import com.saokt.taskmanager.data.local.dao.ProjectDao
import com.saokt.taskmanager.data.local.dao.ProjectInvitationDao
import com.saokt.taskmanager.data.local.dao.ProjectMemberDao
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.local.dao.UserDao
import com.saokt.taskmanager.data.local.entity.ProjectEntity
import com.saokt.taskmanager.data.local.entity.ProjectInvitationEntity
import com.saokt.taskmanager.data.local.entity.ProjectMemberEntity
import com.saokt.taskmanager.data.local.entity.TaskEntity
import com.saokt.taskmanager.data.local.entity.UserEntity
import com.saokt.taskmanager.data.remote.dto.ProjectDto // Need DTOs for mapping
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto // Need DTOs for mapping
// Removed FirebaseAuthSource import, assume firebaseAuthSource provides user ID correctly
import com.saokt.taskmanager.data.remote.firebase.FirebaseUserSource
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Subtask
import com.saokt.taskmanager.domain.model.SyncStatus
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.delay
import android.database.sqlite.SQLiteConstraintException // More specific catch
import com.saokt.taskmanager.data.remote.firebase.FirebaseAuthSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue // Alternative for queues

// Assuming firebaseAuthSource is correctly injected and provides user ID
// Might need to adjust how currentUserId is obtained if not from auth source directly
import com.google.firebase.auth.FirebaseAuth // Example if using direct auth

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

    // --- In-Memory Retry Queues ---
    // Using ConcurrentLinkedQueue for better thread safety if retries happen concurrently
    private val taskRetryQueue = ConcurrentLinkedQueue<TaskDto>()
    private val invitationRetryQueue = ConcurrentLinkedQueue<ProjectInvitationDto>()
    private val retryMutex = Mutex() // To prevent concurrent modification issues during retry logic

    // --- Auth Methods (Remove fetchUserData call as done previously) ---
    override suspend fun signIn(email: String, password: String): Result<User> {
        // ... (implementation without fetchUserData call) ...
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

            Log.d("UserRepository", "User updated successfully: ${user.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user: ${e.message}", e)
            Result.failure(e)
        }
    }


    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        // ... (implementation without fetchUserData call) ...
        val result = firebaseAuthSource.signInWithGoogle(idToken)
        return result.mapCatching { userDto ->
            userDao.insertUser(UserEntity(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl))
            User(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl)
        }
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        // ... (implementation remains the same) ...
        val result = firebaseAuthSource.signUp(email, password, displayName)
        return result.mapCatching { userDto ->
            userDao.insertUser(UserEntity(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl))
            User(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl)
        }
    }



    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { entity ->
            entity?.let { User(it.id, it.email, it.displayName, it.photoUrl) }
        }
    }

    override fun isUserAuthenticated(): Boolean {
        return firebaseAuthSource.getCurrentUser() != null
    }

    // --- NEW Listener Flows Exposed by Repository ---


    // --- Auth Methods (Remain largely the same) ---
    // ... (signIn, signInWithGoogle, signUp, signOut, getCurrentUser, isUserAuthenticated) ...
    // Ensure signOut clears the retry queues as well
    override suspend fun signOut(): Result<Unit> {
        val result = firebaseAuthSource.signOut() // Assuming this is the correct call now
        if (result.isSuccess) {
            Log.d("UserRepository", "Clearing local data on sign out")
            // Clear local data
            userDao.deleteAllUsers()
            projectDao.deleteAllProjects()
            taskDao.deleteAllTasks()
            invitationDao.deleteAllInvitations() // Make sure this exists
            projectMemberDao.deleteAllMembers() // Make sure this exists

            // Clear retry queues
            taskRetryQueue.clear()
            invitationRetryQueue.clear()
            Log.d("UserRepository", "Cleared retry queues on sign out")
        }
        return result
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val result = firebaseAuthSource.deleteAccount()
            if (result.isSuccess) {
                Log.d("UserRepository", "Account deleted successfully, clearing local data")
                // Clear all local data after successful account deletion
                userDao.deleteAllUsers()
                projectDao.deleteAllProjects()
                taskDao.deleteAllTasks()
                invitationDao.deleteAllInvitations()
                projectMemberDao.deleteAllMembers()

                // Clear retry queues
                taskRetryQueue.clear()
                invitationRetryQueue.clear()
                Log.d("UserRepository", "Cleared all local data after account deletion")
            }
            result
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to delete account", e)
            Result.failure(e)
        }
    }

    // --- NEW Listener Flows Exposed by Repository ---
    override fun listenToRemoteProjects(): Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>> {
        return firebaseUserSource.listenToUserProjects()
    }

    override fun listenToRemoteTasks(): Flow<Result<List<TaskDto>>> {
        return firebaseUserSource.listenToUserTasks()
    }

    override fun listenToRemoteInvitations(): Flow<Result<List<ProjectInvitationDto>>> {
        return firebaseUserSource.listenToInvitations()
    }

    // --- Helper function to retry operations ---
    // Keep this for potential direct DAO errors, but primary logic relies on pre-filtering
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
            } catch (e: SQLiteConstraintException) { // Catch specific constraint errors
                attempt++
                if (attempt >= maxAttempts || !e.message.orEmpty().contains("FOREIGN KEY constraint failed", ignoreCase = true)) {
                    Log.e("UserRepository", "Caught non-FK constraint or max retries reached on attempt $attempt: ${e.message}", e)
                    throw e // Rethrow if not FK error or max attempts reached
                }
                Log.w("UserRepository", "Foreign key constraint failed (attempt $attempt/$maxAttempts), retrying in ${currentDelay}ms...", e)
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(5000) // Exponential backoff, capped
            } catch (e: Exception) {
                Log.e("UserRepository", "Caught other exception during retryable operation (attempt $attempt): ${e.message}", e)
                throw e // Rethrow other exceptions immediately
            }
        }
    }

    // --- Sync Logic ---

    // Sync Members (Keep existing logic, assumes projects are handled separately)
    @Transaction
    override suspend fun syncRemoteMembersToLocal(memberDtos: List<ProjectMemberDto>) {
        Log.d("UserRepository", "Syncing ${memberDtos.size} remote project members to local DB")
        if (memberDtos.isEmpty()) {
            Log.d("UserRepository", "No members to sync.")
            // Optional: Still perform deletion logic if needed even with empty remote list
        }

        try {
            // We still wrap the core logic in withRetry as a fallback
            withRetry {
                // 1. Map DTOs to Entities (Only map necessary items)
                val memberEntities = memberDtos.map { dto ->
                    ProjectMemberEntity(
                        projectId = dto.projectId,
                        userId = dto.userId, // Corrected field name from userld
                        email = dto.email,
                        displayName = dto.displayName,
                        role = dto.role
                    )
                }

                // 2. Get unique project IDs from the received members
                val projectIdsWithMembers = memberDtos.map { it.projectId }.distinct().toSet()

                // 3. Get current members in local DB FOR THESE PROJECTS ONLY
                val currentLocalMembers = if (projectIdsWithMembers.isNotEmpty()) {
                    projectMemberDao.getMembersForProjectsList(projectIdsWithMembers.toList()) // Assuming this DAO method exists
                } else {
                    emptyList()
                }


                // 4. Calculate members to delete (present locally for these projects, but not in remote list)
                val membersToDelete = currentLocalMembers.filter { localMember ->
                    memberEntities.none { remoteEntity ->
                        remoteEntity.projectId == localMember.projectId && remoteEntity.userId == localMember.userId
                    }
                }

                if (membersToDelete.isNotEmpty()) {
                    Log.d("UserRepository", "Deleting ${membersToDelete.size} local project members for projects: $projectIdsWithMembers")
                    projectMemberDao.deleteMembers(membersToDelete)
                }

                // 5. Verify projects exist before adding members
                val existingProjectIds = projectDao.getAllLocalProjectIds().toSet()
                val (validMemberEntities, skippedMembers) = memberEntities.partition {
                    existingProjectIds.contains(it.projectId)
                }

                if (skippedMembers.isNotEmpty()) {
                    Log.w("UserRepository", "Skipping ${skippedMembers.size} members due to non-existent projects: ${skippedMembers.map { it.projectId to it.userId }}")
                    // NOTE: We are NOT adding skipped members to a retry queue here,
                    // assuming member sync happens AFTER project sync usually.
                    // If members could arrive before projects consistently, a retry queue for members might be needed too.
                }

                // 6. Upsert the valid entities received from remote
                if (validMemberEntities.isNotEmpty()) {
                    Log.d("UserRepository", "Upserting ${validMemberEntities.size} project members locally for projects: $projectIdsWithMembers")
                    projectMemberDao.upsertAll(validMemberEntities)
                } else {
                    Log.d("UserRepository", "No valid project members to upsert locally for projects: $projectIdsWithMembers")
                }
            } // End withRetry
            Log.d("UserRepository", "Project member sync transaction complete")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during project member sync transaction", e)
            throw e // Rethrow or handle
        }
    }


    // Sync Invitations (Modified with Retry Queue)
    @Transaction
    override suspend fun syncRemoteInvitationsToLocal(projectInvitationDtos: List<ProjectInvitationDto>) {
        val currentUserId = firebaseAuth.currentUser?.uid // Get current user ID reliably
        if (currentUserId == null) {
            Log.e("UserRepository", "Cannot sync invitations, user not authenticated.")
            invitationRetryQueue.clear()
            return
        }
        Log.d("UserRepository", "Syncing ${projectInvitationDtos.size} remote invitations for user $currentUserId to local DB")

        try {
            // Map all received invitations to entities, regardless of project presence
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

            // Calculate IDs to delete
            val remoteInvitationIds = projectInvitationDtos.map { it.id }.toSet()
            val localInvitationIds = invitationDao.getAllLocalInvitationIds().toSet()
            val idsToDelete = localInvitationIds - remoteInvitationIds

            withRetry {
                if (idsToDelete.isNotEmpty()) {
                    Log.d("UserRepository", "Deleting ${idsToDelete.size} local invitations for user $currentUserId: $idsToDelete")
                    invitationDao.deleteInvitationsByIds(idsToDelete.toList())
                }
                if (invitationEntities.isNotEmpty()) {
                    Log.d("UserRepository", "Upserting ${invitationEntities.size} invitations locally for user $currentUserId.")
                    invitationDao.upsertAllInvitations(invitationEntities)
                } else {
                    Log.d("UserRepository", "No invitations to upsert locally for user $currentUserId.")
                }
            }
            Log.d("UserRepository", "Invitation sync transaction complete for user $currentUserId.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error during invitation sync transaction for user $currentUserId", e)
            throw e
        }
    }


    // Sync Projects (Triggers Retries)
    @Transaction
    override suspend fun syncRemoteProjectsToLocal(projectDtos: List<ProjectDto>) {
        Log.d("UserRepository", "Syncing ${projectDtos.size} remote projects to local DB")
        var projectsUpserted = false
        try {
            // 1. Map DTOs to Entities
            val projectEntities = projectDtos.map { dto ->
                ProjectEntity(
                    id = dto.id, // Ensure ID is mapped
                    title = dto.title,
                    description = dto.description,
                    color = dto.color,
                    startDate = dto.startDate,
                    dueDate = dto.dueDate,
                    isCompleted = dto.completed,
                    createdAt = dto.createdAt,
                    modifiedAt = dto.modifiedAt,
                    syncStatus = SyncStatus.SYNCED, // Mark as synced
                    ownerId = dto.ownerId // Corrected field name from ownerld
                    // Map members if needed/stored differently in Room
                )
            }

            // 2. Get IDs from remote
            val remoteProjectIds = projectDtos.map { it.id }.toSet()

            // 3. Get IDs currently in local DB
            val localProjectIds = projectDao.getAllLocalProjectIds().toSet()

            // 4. Calculate IDs to delete
            val idsToDelete = localProjectIds - remoteProjectIds

            // Perform Deletion and Upsert within a retry block as a fallback
            withRetry {
                // 5. Delete projects no longer present remotely
                if (idsToDelete.isNotEmpty()) {
                    Log.d("UserRepository", "Deleting ${idsToDelete.size} projects locally: $idsToDelete")
                    projectDao.deleteByIds(idsToDelete.toList())
                    // Cascading deletes should handle related tasks/members/invites in Room if set up
                }

                // 6. Upsert the entities received from remote
                if (projectEntities.isNotEmpty()) {
                    Log.d("UserRepository", "Upserting ${projectEntities.size} projects locally.")
                    projectDao.upsertAll(projectEntities)
                    projectsUpserted = true // Mark that projects were potentially added/updated
                } else {
                    Log.d("UserRepository", "No projects to upsert locally.")
                }
            } // End withRetry

            Log.d("UserRepository", "Project sync transaction complete.")

            // --- TRIGGER RETRIES ---
            // If projects were successfully upserted, try processing pending items
            if (projectsUpserted || !taskRetryQueue.isEmpty() || !invitationRetryQueue.isEmpty()) {
                Log.d("UserRepository", "Project sync successful, triggering retries for pending tasks/invitations...")
                retryPendingTasks()
                retryPendingInvitations()
            }

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during project sync transaction", e)
            throw e
        }
    }


    // Sync Tasks (Modified with Retry Queue)
    @Transaction
    override suspend fun syncRemoteTasksToLocal(taskDtos: List<TaskDto>) {
        Log.d("UserRepository", "Syncing ${taskDtos.size} remote tasks to local DB")

        try {
            // 1. Get existing Project IDs first
            val existingProjectIds = projectDao.getAllLocalProjectIds().toSet()

            // 2. Filter tasks: valid vs pending retry
            val validTaskDtos = mutableListOf<TaskDto>()
            val newlyPendingTasks = mutableListOf<TaskDto>()

            for (dto in taskDtos) {
                // Check if the referenced project exists (allow tasks with null projectId)
                if (dto.projectId == null || existingProjectIds.contains(dto.projectId)) {
                    validTaskDtos.add(dto)
                } else {
                    // Project doesn't exist locally yet, add to retry queue
                    newlyPendingTasks.add(dto)
                }
            }

            // Add newly pending tasks to the main queue
            if (newlyPendingTasks.isNotEmpty()) {
                retryMutex.withLock {
                    newlyPendingTasks.forEach { dto ->
                        if (!taskRetryQueue.contains(dto)) { // Avoid duplicates
                            taskRetryQueue.offer(dto)
                            Log.w("UserRepository", "Task ${dto.id} (Project ${dto.projectId}) needs project. Added to retry queue.")
                        }
                    }
                    Log.d("UserRepository", "Task retry queue size: ${taskRetryQueue.size}")
                }
            }

            // Log first few valid tasks for verification
            validTaskDtos.take(3).forEachIndexed { index, dto ->
                Log.d("UserRepository", "Sample valid task $index: id=${dto.id}, title=${dto.title}, projectId=${dto.projectId}")
            }
            Log.d("UserRepository", "Processing ${validTaskDtos.size} valid tasks with project references.")


            // 3. Map valid DTOs to Entities
            val taskEntities = validTaskDtos.map { dto ->
                // Use TaskMapper to ensure all fields are properly mapped
                val task = taskMapper.dtoToDomain(dto)
                taskMapper.domainToEntity(task)
            }


            // 4. Calculate IDs to delete
            val remoteTaskIds = taskDtos.map { it.id }.toSet() // All received task IDs
            val localTaskIds = taskDao.getAllLocalTaskIds().toSet()
            val idsToDelete = localTaskIds - remoteTaskIds

            // Perform Deletion and Upsert within a retry block
            withRetry {
                // 5. Delete local tasks no longer present remotely
                if (idsToDelete.isNotEmpty()) {
                    Log.d("UserRepository", "Deleting ${idsToDelete.size} local tasks: $idsToDelete")
                    val deleteCount = taskDao.deleteByIds(idsToDelete.toList()) // Use existing DAO method
                    Log.d("UserRepository", "Actual tasks deleted count: $deleteCount")
                }

                // 6. Upsert the valid entities received from remote
                if (taskEntities.isNotEmpty()) {
                    Log.d("UserRepository", "Upserting ${taskEntities.size} valid tasks locally.")
                    val upsertResult = taskDao.upsertAll(taskEntities) // Use existing DAO method
                    // upsertResult might be List<Long> of row IDs, or Unit depending on DAO
                    Log.d("UserRepository", "Task upsert completed.") // Affected rows info might not be available directly from upsertAll
                } else if (taskDtos.isNotEmpty() && validTaskDtos.isEmpty()) {
                    Log.d("UserRepository", "No currently valid tasks to upsert, all pending project availability.")
                }
                else {
                    Log.d("UserRepository", "No tasks to upsert locally.")
                }
            } // End withRetry

            Log.d("UserRepository", "Task sync transaction complete.")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during task sync transaction", e)
            throw e
        }
    }


    // --- Retry Logic Implementation ---

    private suspend fun retryPendingTasks() {
        retryMutex.withLock { // Prevent concurrent modification/retry attempts
            if (taskRetryQueue.isEmpty()) {
                // Log.d("UserRepository", "Task retry queue is empty.")
                return
            }

            Log.i("UserRepository", "Attempting to retry ${taskRetryQueue.size} pending tasks...")
            val currentlyExistingProjectIds = projectDao.getAllLocalProjectIds().toSet()
            val tasksToRetryNow = mutableListOf<TaskDto>()
            val tasksStillPending = ConcurrentLinkedQueue<TaskDto>() // Use new queue for remaining items

            // Drain the current queue for processing
            while(taskRetryQueue.isNotEmpty()) {
                val taskDto = taskRetryQueue.poll() ?: continue // Get and remove head

                if (taskDto.projectId == null || currentlyExistingProjectIds.contains(taskDto.projectId)) {
                    // Project dependency met (or no dependency)
                    tasksToRetryNow.add(taskDto)
                } else {
                    // Project still missing, add to the *next* retry queue
                    tasksStillPending.offer(taskDto)
                }
            }

            // Replace the old queue with the one containing items still pending
            // This is inherently done by draining and only adding back pending ones below,
            // but if using MutableList, you'd clear and addAll(tasksStillPending)

            if (tasksToRetryNow.isNotEmpty()) {
                Log.i("UserRepository", "Retrying ${tasksToRetryNow.size} tasks whose projects are now available.")
                val retryEntities = tasksToRetryNow.map { dto -> /* ... Map DTO to Entity ... */
                    TaskEntity( /* ... mapping ... */
                        id = dto.id, title = dto.title, description = dto.description, dueDate = dto.dueDate, completed = dto.completed,
                        priority = try { Priority.valueOf(dto.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM },
                        projectId = dto.projectId, labels = dto.labels ?: emptyList(),
                        subtasks = dto.subtasks?.mapNotNull { subtaskMap ->
                            try {
                                Subtask(
                                    id = subtaskMap["id"] as? String ?: return@mapNotNull null, // Require ID
                                    title = subtaskMap["title"] as? String ?: "",
                                    isCompleted = subtaskMap["isCompleted"] as? Boolean ?: false
                                )
                            } catch (e: Exception) {
                                Log.e("UserRepository", "Error mapping subtask: $subtaskMap", e)
                                null
                            }
                        } ?: emptyList(),
                        createdAt = dto.createdAt, modifiedAt = dto.modifiedAt, syncStatus = SyncStatus.SYNCED, userId = dto.userId
                    )
                }
                try {
                    // Retry the upsert for these specific tasks
                    withRetry(maxAttempts = 2, initialDelay = 100) { // Fewer attempts for retry
                        taskDao.upsertAll(retryEntities)
                    }
                    Log.i("UserRepository", "Successfully retried and upserted ${retryEntities.size} tasks.")
                } catch (e: Exception) {
                    Log.e("UserRepository", "Error during task retry upsert for ${retryEntities.size} tasks. Adding back to queue.", e)
                    // If retry fails, add them back to the pending queue for the *next* cycle
                    tasksToRetryNow.forEach { tasksStillPending.offer(it) }
                }
            }

            // Add any remaining tasks (those whose projects were *still* missing) back into the main queue
            taskRetryQueue.addAll(tasksStillPending)

            if (taskRetryQueue.isNotEmpty()) {
                Log.w("UserRepository", "${taskRetryQueue.size} tasks still pending retry.")
            } else {
                Log.i("UserRepository", "Task retry queue is now empty after attempt.")
            }
        }
    }

    private suspend fun retryPendingInvitations() {
        retryMutex.withLock {
            if (invitationRetryQueue.isEmpty()) {
                // Log.d("UserRepository", "Invitation retry queue is empty.")
                return
            }

            Log.i("UserRepository", "Attempting to retry ${invitationRetryQueue.size} pending invitations...")
            val currentlyExistingProjectIds = projectDao.getAllLocalProjectIds().toSet()
            val invitationsToRetryNow = mutableListOf<ProjectInvitationDto>()
            val invitationsStillPending = ConcurrentLinkedQueue<ProjectInvitationDto>()

            while(invitationRetryQueue.isNotEmpty()) {
                val invDto = invitationRetryQueue.poll() ?: continue

                if (currentlyExistingProjectIds.contains(invDto.projectId)) {
                    invitationsToRetryNow.add(invDto)
                } else {
                    invitationsStillPending.offer(invDto)
                }
            }


            if (invitationsToRetryNow.isNotEmpty()) {
                Log.i("UserRepository", "Retrying ${invitationsToRetryNow.size} invitations whose projects are now available.")
                val retryEntities = invitationsToRetryNow.map { dto -> /* ... Map DTO to Entity ... */
                    ProjectInvitationEntity( /* ... mapping ... */
                        id = dto.id, projectId = dto.projectId, projectTitle = dto.projectTitle, inviterId = dto.inviterId,
                        inviterName = dto.inviterName, inviteeId = dto.inviteeId, inviteeEmail = dto.inviteeEmail,
                        status = dto.status, createdAt = dto.createdAt /*, userId = currentUserId if needed */
                    )
                }
                try {
                    withRetry(maxAttempts = 2, initialDelay = 100) {
                        invitationDao.upsertAllInvitations(retryEntities)
                    }
                    Log.i("UserRepository", "Successfully retried and upserted ${retryEntities.size} invitations.")
                } catch (e: Exception) {
                    Log.e("UserRepository", "Error during invitation retry upsert for ${retryEntities.size}. Adding back to queue.", e)
                    invitationsToRetryNow.forEach { invitationsStillPending.offer(it) }
                }
            }

            invitationRetryQueue.addAll(invitationsStillPending)

            if (invitationRetryQueue.isNotEmpty()) {
                Log.w("UserRepository", "${invitationRetryQueue.size} invitations still pending retry.")
            } else {
                Log.i("UserRepository", "Invitation retry queue is now empty after attempt.")
            }
        }
    }
}