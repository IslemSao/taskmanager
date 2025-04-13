package com.example.taskmanager.data.repository

import android.util.Log
import androidx.room.Transaction // Import Transaction
import com.example.taskmanager.data.local.dao.ProjectDao
import com.example.taskmanager.data.local.dao.ProjectInvitationDao
import com.example.taskmanager.data.local.dao.ProjectMemberDao
import com.example.taskmanager.data.local.dao.TaskDao
import com.example.taskmanager.data.local.dao.UserDao
import com.example.taskmanager.data.local.entity.ProjectEntity
import com.example.taskmanager.data.local.entity.ProjectInvitationEntity
import com.example.taskmanager.data.local.entity.ProjectMemberEntity
import com.example.taskmanager.data.local.entity.TaskEntity
import com.example.taskmanager.data.local.entity.UserEntity
import com.example.taskmanager.data.remote.dto.ProjectDto // Need DTOs for mapping
import com.example.taskmanager.data.remote.dto.ProjectInvitationDto
import com.example.taskmanager.data.remote.dto.ProjectMemberDto
import com.example.taskmanager.data.remote.dto.TaskDto    // Need DTOs for mapping
import com.example.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.example.taskmanager.data.remote.firebase.FirebaseUserSource
import com.example.taskmanager.domain.model.Priority
import com.example.taskmanager.domain.model.Subtask
import com.example.taskmanager.domain.model.SyncStatus
import com.example.taskmanager.domain.model.User
import com.example.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val invitationDao: ProjectInvitationDao, // Add DAO for invitations
    // private val projectMemberDao: ProjectMemberDao, // Keep if used elsewhere
    private val firebaseAuthSource: FirebaseAuthSource,
    private val projectMemberDao: ProjectMemberDao, // Add DAO for project members
    private val firebaseUserSource: FirebaseUserSource,

) : UserRepository {

    // --- Auth Methods (Remove fetchUserData call as done previously) ---
    override suspend fun signIn(email: String, password: String): Result<User> {
        // ... (implementation without fetchUserData call) ...
        val result = firebaseAuthSource.signIn(email, password)
        return result.mapCatching { userDto ->
            userDao.insertUser(UserEntity(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl))
            User(userDto.id, userDto.email, userDto.displayName, userDto.photoUrl)
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


    override suspend fun signOut(): Result<Unit> {
        val result = firebaseAuthSource.signOut()
        if (result.isSuccess) {
            Log.d("UserRepository", "Clearing local data on sign out")
            userDao.deleteAllUsers()
            projectDao.deleteAllProjects() // Clear projects for logged-out user
            taskDao.deleteAllTasks()       // Clear tasks for logged-out user
            invitationDao
        }
        return result
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

    override fun listenToRemoteProjects():Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>>{
        // Directly expose the flow from the source
        return firebaseUserSource.listenToUserProjects()
    }



    override fun listenToRemoteTasks(): Flow<Result<List<TaskDto>>> {
        // Directly expose the flow from the source
        return firebaseUserSource.listenToUserTasks()
    }

    override fun listenToRemoteInvitations(): Flow<Result<List<ProjectInvitationDto>>> {
        // Directly expose the flow from the source
        return firebaseUserSource.listenToInvitations()
    }

    @Transaction
    override suspend fun syncRemoteMembersToLocal(memberDtos: List<ProjectMemberDto>) {
        try {
            Log.d("UserRepository", "Syncing ${memberDtos.size} remote project members to local DB")

            // 1. Map DTOs to Entities
            val memberEntities = memberDtos.map { dto ->
                ProjectMemberEntity(
                    projectId = dto.projectId,
                    userId = dto.userId,
                    email = dto.email,
                    displayName = dto.displayName,
                    role = dto.role
                )
            }

            // 2. Get unique project IDs from the received members
            val projectIdsWithMembers = memberDtos.map { it.projectId }.distinct()

            // 3. Get current members in local DB for these projects
            val currentLocalMembers = projectMemberDao.getMembersForProjects(projectIdsWithMembers)

            // 4. Calculate members to delete
            // These are members present locally but not in the latest list from Firestore
            val membersToDelete = currentLocalMembers.filter { localMember ->
                memberDtos.none { remoteMember ->
                    remoteMember.projectId == localMember.projectId &&
                            remoteMember.userId == localMember.userId
                }
            }

            if (membersToDelete.isNotEmpty()) {
                Log.d("UserRepository", "Deleting ${membersToDelete.size} local project members")
                projectMemberDao.deleteMembers(membersToDelete)
            } else {
                Log.d("UserRepository", "No project members to delete locally")
            }

            // 5. Upsert the entities received from remote
            if (memberEntities.isNotEmpty()) {
                // This will insert new members and update existing ones based on their 'projectId' and 'userId'
                Log.d("UserRepository", "Upserting ${memberEntities.size} project members locally")
                Log.d("UserRepository", "project members $memberEntities ")
                projectMemberDao.upsertAll(memberEntities)
            } else {
                Log.d("UserRepository", "No project members to upsert locally")
            }

            Log.d("UserRepository", "Project member sync transaction complete")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during project member sync transaction", e)
            // Handle error appropriately (re-throw, log, etc.)
            throw e // or handle differently based on your needs
        }
    }

    @Transaction
    override suspend fun syncRemoteInvitationsToLocal(projectInvitationDtos: List<ProjectInvitationDto>) {
        // Get current user ID - needed if your local table stores invites for multiple users
        val currentUserId = firebaseAuthSource.getCurrentUserId() // Or get it from where you store it
        if (currentUserId == null) {
            Log.e("UserRepository", "Cannot sync invitations, user not authenticated.")
            // Optionally throw an exception or return early depending on desired behavior
            return
        }

        try {
            Log.d("UserRepository", "Syncing ${projectInvitationDtos.size} remote invitations for user $currentUserId to local DB")

            // 1. Map DTOs to Entities
            val invitationEntities = projectInvitationDtos.map { dto ->
                ProjectInvitationEntity(
                    id = dto.id,
                    projectId = dto.projectId,
                    projectTitle = dto.projectTitle,
                    inviterId = dto.inviterId,
                    inviterName = dto.inviterName,
                    inviteeId = dto.inviteeId, // Should match currentUserId for invites received via listener
                    inviteeEmail = dto.inviteeEmail,
                    status = dto.status,
                    createdAt = dto.createdAt,

                )
            }

            // 2. Get IDs from remote (received list)
            val remoteInvitationIds = projectInvitationDtos.map { it.id }.toSet()

            // 3. Get IDs currently in local DB (for the specific user)
            // Adjust the DAO call based on your DAO definition
            val localInvitationIds = invitationDao.getAllLocalInvitationIdsForUser(currentUserId).toSet()
            // OR: val localInvitationIds = invitationDao.getAllLocalInvitationIds().toSet()

            // 4. Calculate IDs to delete locally
            // These are IDs present locally but NOT in the latest list from Firestore
            val idsToDelete = localInvitationIds - remoteInvitationIds
            if (idsToDelete.isNotEmpty()) {
                Log.d("UserRepository", "Deleting ${idsToDelete.size} local invitations for user $currentUserId: $idsToDelete")
                invitationDao.deleteInvitationsByIds(idsToDelete.toList())
            } else {
                Log.d("UserRepository", "No invitations to delete locally for user $currentUserId.")
            }

            // 5. Upsert the entities received from remote
            // This will insert new invitations and update existing ones based on their 'id'
            if (invitationEntities.isNotEmpty()) {
                Log.d("UserRepository", "Upserting ${invitationEntities.size} invitations locally for user $currentUserId.")
                invitationDao.upsertAllInvitations(invitationEntities)
            } else {
                Log.d("UserRepository", "No invitations to upsert locally for user $currentUserId.")
            }

            Log.d("UserRepository", "Invitation sync transaction complete for user $currentUserId.")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during invitation sync transaction for user $currentUserId", e)
            // Decide how to handle transaction errors: re-throw, log, maybe update sync status of related items to ERROR?
        }
    }

    // --- NEW Sync Logic (Called when listener flows emit data) ---

    @Transaction // Ensure atomicity
    override suspend fun syncRemoteProjectsToLocal(projectDtos: List<ProjectDto>) {
        try {
            Log.d("UserRepository", "Syncing ${projectDtos.size} remote projects to local DB")
            // 1. Map DTOs to Entities
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
                    syncStatus = SyncStatus.SYNCED, // Mark as synced
                    ownerId = dto.ownerId
                    // Map members if needed/stored differently in Room
                )
            }

            // 2. Get IDs from remote
            val remoteProjectIds = projectDtos.map { it.id }.toSet()

            // 3. Get IDs currently in local DB
            val localProjectIds = projectDao.getAllLocalProjectIds().toSet() // Use Set for efficient difference

            // 4. Calculate IDs to delete
            val idsToDelete = localProjectIds - remoteProjectIds
            if (idsToDelete.isNotEmpty()) {
                Log.d("UserRepository", "Deleting ${idsToDelete.size} projects locally: $idsToDelete")
                projectDao.deleteByIds(idsToDelete.toList())
            } else {
                Log.d("UserRepository", "No projects to delete locally.")
            }


            // 5. Upsert the entities received from remote
            if (projectEntities.isNotEmpty()) {
                Log.d("UserRepository", "Upserting ${projectEntities.size} projects locally.")
                projectDao.upsertAll(projectEntities)
            } else {
                Log.d("UserRepository", "No projects to upsert locally.")
            }

            Log.d("UserRepository", "Project sync transaction complete.")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during project sync transaction", e)
            // How to handle transaction errors? Maybe re-throw or log.
        }
    }

    @Transaction
    override suspend fun syncRemoteTasksToLocal(taskDtos: List<TaskDto>) {
        try {
            Log.d("UserRepository", "Syncing ${taskDtos.size} remote tasks to local DB")

            // Log first few tasks to verify data
            taskDtos.take(3).forEachIndexed { index, dto ->
                Log.d("UserRepository", "Sample task $index: id=${dto.id}, title=${dto.title}, projectId=${dto.projectId}")
            }

            // 1. Map DTOs to Entities
            val taskEntities = taskDtos.map { dto ->
                TaskEntity(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    dueDate = dto.dueDate,
                    completed = dto.completed,
                    priority = try { Priority.valueOf(dto.priority) } catch (e: IllegalArgumentException) {
                        Log.w("UserRepository", "Invalid priority '${dto.priority}' for task ${dto.id}, defaulting to MEDIUM")
                        Priority.MEDIUM
                    },
                    projectId = dto.projectId,
                    labels = dto.labels,
                    subtasks = dto.subtasks.mapNotNull { subtaskMap ->
                        try {
                            Subtask(
                                id = subtaskMap["id"] as String,
                                title = subtaskMap["title"] as String,
                                isCompleted = subtaskMap["isCompleted"] as Boolean
                            )
                        } catch (e: Exception) {
                            Log.e("UserRepository", "Error mapping subtask: $subtaskMap", e)
                            null
                        }
                    },
                    createdAt = dto.createdAt,
                    modifiedAt = dto.modifiedAt,
                    syncStatus = SyncStatus.SYNCED
                ).also {
                    Log.v("UserRepository", "Mapped task entity: ${it.id} with ${it.subtasks.size} subtasks")
                }
            }

            // 2. Get IDs from remote
            val remoteTaskIds = taskDtos.map { it.id }.toSet()
            Log.d("UserRepository", "Remote task IDs count: ${remoteTaskIds.size}")

            // 3. Get IDs currently in local DB
            val localTaskIds = taskDao.getAllLocalTaskIds().toSet()
            Log.d("UserRepository", "Local task IDs count: ${localTaskIds.size}")

            // 4. Calculate IDs to delete
            val idsToDelete = localTaskIds - remoteTaskIds
            if (idsToDelete.isNotEmpty()) {
                Log.d("UserRepository", "Deleting ${idsToDelete.size} tasks locally: $idsToDelete")
                val deleteCount = taskDao.deleteByIds(idsToDelete.toList())
                Log.d("UserRepository", "Actual deleted count: $deleteCount")
            } else {
                Log.d("UserRepository", "No tasks to delete locally.")
            }

            // 5. Upsert the entities received from remote
            if (taskEntities.isNotEmpty()) {
                Log.d("UserRepository", "Upserting ${taskEntities.size} tasks locally.")
                val upsertCount = taskDao.upsertAll(taskEntities)
                Log.d("UserRepository", "Upsert completed, affected rows: $upsertCount")

            } else {
                Log.d("UserRepository", "No tasks to upsert locally.")
            }

            Log.d("UserRepository", "Task sync transaction complete.")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error during task sync transaction", e)
        }
    }

    // Remove or keep the old fetchUserData if needed for manual sync?
    // For now, let's assume it's removed or repurposed.
    // override suspend fun fetchUserData(userId: String, userEmail: String): Result<Unit> { ... }
}
