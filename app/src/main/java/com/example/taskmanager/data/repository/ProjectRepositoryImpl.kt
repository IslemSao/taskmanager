package com.example.taskmanager.data.repository

import android.util.Log
import com.example.taskmanager.data.local.dao.ProjectDao
import com.example.taskmanager.data.local.dao.ProjectInvitationDao
import com.example.taskmanager.data.local.dao.ProjectMemberDao
import com.example.taskmanager.data.mapper.ProjectInvitationMapper
import com.example.taskmanager.data.mapper.ProjectMapper
import com.example.taskmanager.data.mapper.ProjectMemberMapper
import com.example.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.example.taskmanager.data.remote.firebase.FirebaseProjectSource
import com.example.taskmanager.domain.model.InvitationStatus
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.ProjectInvitation
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.model.ProjectRole
import com.example.taskmanager.domain.model.SyncStatus
import com.example.taskmanager.domain.repository.ProjectRepository
import com.example.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectMemberDao: ProjectMemberDao,
    private val projectInvitationDao: ProjectInvitationDao,
    private val firebaseProjectSource: FirebaseProjectSource,
    private val firebaseAuthSource: FirebaseAuthSource,
    private val projectMapper: ProjectMapper,
    private val projectMemberMapper: ProjectMemberMapper,
    private val projectInvitationMapper: ProjectInvitationMapper,
    private val userRepository: UserRepository
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { projectMapper.entityToDomain(it) }
        }
    }

    override fun getProjectById(projectId: String): Flow<Project?> {
        return projectDao.getProjectById(projectId).map { entity ->
            entity?.let { projectMapper.entityToDomain(it) }
        }
    }

    override suspend fun createProject(project: Project): Result<Project> {
        val projectToSave = project.copy(
            createdAt = Date(),
            modifiedAt = Date(),
            syncStatus = SyncStatus.PENDING
        )

        val entity = projectMapper.domainToEntity(projectToSave)
        projectDao.insertProject(entity)

        tryImmediateSync(projectToSave)

        Log.d("addProjectDebug", "createProject in repoIML: $projectToSave")
        return Result.success(projectToSave)
    }

    private suspend fun tryImmediateSync(project: Project) {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                val dto = projectMapper.domainToDto(project).copy(ownerId = currentUser.id)
                firebaseProjectSource.updateProject(dto)
                // If successful, update the sync status in Room
                projectDao.updateSyncStatus(project.id, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            // Log the error but don't fail - the background sync will handle it later
            Log.e("ProjectRepository", "Failed immediate sync: ${e.message}")
        }
    }


    override suspend fun updateProject(project: Project): Result<Project> {
        val updatedProject = project.copy(
            modifiedAt = Date(),
            syncStatus = SyncStatus.PENDING
        )

        val entity = projectMapper.domainToEntity(updatedProject)
        projectDao.updateProject(entity)

        return Result.success(updatedProject)
    }


    override suspend fun deleteProject(projectId: String): Result<Unit> {
        val TAG = "DeleteProject"

        return try {
            Log.d(TAG, "Attempting to delete project with ID: $projectId")

            val userId = firebaseAuthSource.getCurrentUserId()
            Log.d(TAG, "Current user ID: $userId")

            val project = projectDao.getProjectById(projectId).firstOrNull()
            if (project == null) {
                Log.e(TAG, "Project not found for ID: $projectId")
                return Result.failure(IllegalStateException("Project not found"))
            }

            Log.d(TAG, "Project retrieved: $project")

            if (userId != project.ownerId) {
                Log.e(TAG, "User does not own the project. userId=$userId, ownerId=${project.ownerId}")
                return Result.failure(IllegalStateException("User doesn't own the project"))
            }

            Log.d(TAG, "User owns the project. Proceeding with deletion.")


            // Then delete from Firebase
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                Log.d(TAG, "Deleting project and related data from Firebase for user: ${currentUser.id}")
                deleteProjectAndRelatedDataFromFirebase(projectId)
                Log.d(TAG, "Firebase deletion complete.")
            } else {
                Log.e(TAG, "Current user is null while trying to delete project from Firebase.")
            }

            Log.d(TAG, "Project deletion completed successfully.")
            // First delete from Room
            projectDao.deleteProject(projectId)
            Log.d(TAG, "Project deleted from local Room database.")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while deleting project: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteProjectAndRelatedDataFromFirebase(projectId: String) {
        try {
            Log.d(
                "deleteProjectDebug",
                "deleteProjectAndRelatedDataFromFirebase in repoIML: $projectId"
            )

            // 1. First delete all tasks in the project
            firebaseProjectSource.deleteAllTasksForProject(projectId)
            Log.d("deleteProjectDebug", "after deleteAllTasksForProject")
            // 2. Then delete all invitations for the project
            firebaseProjectSource.deleteAllInvitationsForProject(projectId)
            Log.d("deleteProjectDebug", "after deleteAllInvitationsForProject")

            // 3. Then delete all members of the project
            firebaseProjectSource.deleteAllMembersForProject(projectId)
            Log.d("deleteProjectDebug", "after deleteAllMembersForProject")

            // 4. Finally delete the project itself
            firebaseProjectSource.deleteProject(projectId)
            Log.d("deleteProjectDebug", "after deleteProject")
        } catch (e: Exception) {
            Log.e(
                "ProjectRepository",
                "Failed to delete project and related data from Firebase: ${e.message}"
            )
            throw e
        }
    }


    override suspend fun completeProject(projectId: String): Result<Project> {
        val projectFlow = projectDao.getProjectById(projectId)
        val projectEntity = projectFlow.firstOrNull() ?: return Result.failure(
            IllegalStateException("Project not found")
        )

        val project = projectMapper.entityToDomain(projectEntity)

        val completedProject = project.copy(
            isCompleted = true,
            modifiedAt = Date(),
            syncStatus = SyncStatus.PENDING
        )

        val entity = projectMapper.domainToEntity(completedProject)
        projectDao.updateProject(entity)

        return Result.success(completedProject)
    }


    // Similar improvements for updateProject and other methods...

    override suspend fun syncPendingProjects(): Result<Int> {
        try {
            val pendingProjects = projectDao.getProjectsBySyncStatus(SyncStatus.PENDING)
            var syncedCount = 0

            val currentUser = userRepository.getCurrentUser().first()
            val userId =
                currentUser?.id ?: return Result.failure(Exception("User not authenticated"))

            pendingProjects.forEach { entity ->
                val dto = projectMapper.entityToDto(entity).copy(id = userId)
                val result = firebaseProjectSource.updateProject(dto)

                if (result.isSuccess) {
                    projectDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    syncedCount++
                } else {
                    projectDao.updateSyncStatus(entity.id, SyncStatus.SYNC_FAILED)
                }
            }

            return Result.success(syncedCount)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun inviteUserToProject(
        projectId: String,
        projectTitle: String,
        userEmail: String
    ): Result<ProjectInvitation> {
        try {
            Log.d("invitation", "repo fun")
            // Get current user
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))

            // Create invitation
            val invitation = ProjectInvitation(
                projectId = projectId,
                projectTitle = projectTitle,
                inviterId = currentUser.id,
                inviterName = currentUser.displayName ?: currentUser.email,
                inviteeId = "",
                inviteeEmail = userEmail,
                createdAt = Date()
            )

            // Save to local database
            val invitationEntity = projectInvitationMapper.domainToEntity(invitation)
            projectInvitationDao.insertInvitation(invitationEntity)

            // Try to sync with Firebase
            try {
                Log.d("invitation", "repo fun try synch")

                val invitationDto = projectInvitationMapper.domainToDto(invitation)
                firebaseProjectSource.createInvitation(invitationDto)
            } catch (e: Exception) {
                Log.d("invitation", "repo fun try synch failed : ${e.message}")
                Log.e("ProjectRepository", "Failed to sync invitation: ${e.message}")
                // We'll retry during background sync
            }

            return Result.success(invitation)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getProjectInvitations(): Flow<List<ProjectInvitation>> {
        Log.d("bombardiro", "from getProjectInvitations")
        return projectInvitationDao.getAllInvitations().map { entities ->
            entities.map { projectInvitationMapper.entityToDomain(it) }
        }
    }

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> {
        try {
            Log.d(
                "Debug",
                "Starting respondToInvitation with id: $invitationId and accept: $accept"
            )

            // Get the invitation
            val invitation = projectInvitationDao.getInvitationById(invitationId).first()
                ?: return Result.failure(Exception("Invitation not found"))
            Log.d("Debug", "Fetched invitation: $invitation")

            val invitationDomain = projectInvitationMapper.entityToDomain(invitation)
            Log.d("Debug", "Mapped invitation to domain: $invitationDomain")

            // Update invitation status
            val updatedInvitation = invitationDomain.copy(
                status = if (accept) InvitationStatus.ACCEPTED else InvitationStatus.REJECTED
            )
            Log.d("Debug", "Updated invitation status: $updatedInvitation")

            // Save to local database
            val invitationEntity = projectInvitationMapper.domainToEntity(updatedInvitation)
            projectInvitationDao.updateInvitation(invitationEntity)
            Log.d("Debug", "Updated invitation in local DB")

            // If accepted, add user as project member
            if (accept) {
                Log.d("Debug", "Invitation accepted, proceeding to add user as project member")
                val currentUser = userRepository.getCurrentUser().first()
                    ?: return Result.failure(Exception("User not authenticated"))
                Log.d("Debug", "Fetched current user: $currentUser")

                val member = ProjectMember(
                    projectId = invitationDomain.projectId,
                    userId = currentUser.id,
                    email = currentUser.email,
                    displayName = currentUser.displayName ?: currentUser.email,
                    role = ProjectRole.MEMBER
                )
                Log.d("Debug", "Created project member: $member")

                Log.d("Debug", "Inserted new project member in local DB")

                try {
                    val memberDto = projectMemberMapper.domainToDto(member)
                    firebaseProjectSource.addProjectMember(invitationDomain.projectId, memberDto)
                    Log.d("Debug", "Synced project member addition to Firebase")
                } catch (e: Exception) {
                    Log.e("ProjectRepository", "Failed to sync member addition: ${e.message}")
                }
            }

            // Try to sync with Firebase
            try {

                Log.d("Debug", "Invitation response: $updatedInvitation")
                val invitationDto = projectInvitationMapper.domainToDto(updatedInvitation)
                Log.d("Debug", "Invitation dto: $invitationDto")
                firebaseProjectSource.updateInvitation(invitationDto)
                Log.d("Debug", "Synced invitation response to Firebase")
            } catch (e: Exception) {
                Log.e("ProjectRepository", "Failed to sync invitation response: ${e.message}")
            }

            Log.d("Debug", "respondToInvitation completed successfully")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Error in respondToInvitation: ${e.message}")
            return Result.failure(e)
        }
    }

    override suspend fun getProjectMembers(projectId: String): Flow<List<ProjectMember>> {
        return projectMemberDao.getMembersByProject(projectId).map { entities ->
            entities.map { projectMemberMapper.entityToDomain(it) }
        }
    }

    override suspend fun removeMemberFromProject(projectId: String, userId: String): Result<Unit> {
        try {
            // Get current user
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))

            // Get the project
            val project = projectDao.getProjectById(projectId).first()
                ?: return Result.failure(Exception("Project not found"))

            val projectDomain = projectMapper.entityToDomain(project)

            // Check if user is the owner
            if (projectDomain.ownerId != currentUser.id) {
                return Result.failure(Exception("Only project owners can remove members"))
            }

            // Remove member from project
            val updatedMembers = projectDomain.members.filter { it.userId != userId }
            val updatedProject = projectDomain.copy(
                members = updatedMembers,
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )

            // Update project in database
            val projectEntity = projectMapper.domainToEntity(updatedProject)
            projectDao.updateProject(projectEntity)

            // Remove member from members table
            projectMemberDao.deleteMember(projectId, userId)

            // Try to sync with Firebase
            try {
                val projectDto = projectMapper.entityToDto(projectEntity)
                firebaseProjectSource.updateProject(projectDto)
            } catch (e: Exception) {
                Log.e("ProjectRepository", "Failed to sync member removal: ${e.message}")
                // We'll retry during background sync
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getProjectMembers(): Flow<List<ProjectMember>> {
        return try {
            projectMemberDao.getAllMembers().map { entities ->
                entities.map { projectMemberMapper.entityToDomain(it) }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}