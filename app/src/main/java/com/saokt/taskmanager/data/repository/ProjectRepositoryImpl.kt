package com.saokt.taskmanager.data.repository

import android.util.Patterns
import android.util.Log
import com.saokt.taskmanager.data.local.dao.ProjectDao
import com.saokt.taskmanager.data.local.dao.ProjectInvitationDao
import com.saokt.taskmanager.data.local.dao.ProjectMemberDao
import com.saokt.taskmanager.data.mapper.ProjectInvitationMapper
import com.saokt.taskmanager.data.mapper.ProjectMapper
import com.saokt.taskmanager.data.mapper.ProjectMemberMapper
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.saokt.taskmanager.data.remote.firebase.FirebaseProjectSource
import com.saokt.taskmanager.domain.model.InvitationStatus
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectInvitation
import com.saokt.taskmanager.domain.model.ProjectMember
import com.saokt.taskmanager.domain.model.ProjectRole
import com.saokt.taskmanager.domain.model.SyncStatus
import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
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
    companion object {
        private const val TAG = "ProjectRepository"
    }

    override fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { projectMapper.entityToDomain(it) }
        }
    }

    override fun getProjectById(projectId: String): Flow<Project?> {
        return projectDao.getProjectById(projectId).map { entity -> entity?.let(projectMapper::entityToDomain) }
    }

    override suspend fun createProject(project: Project): Result<Project> {
        val currentUser = userRepository.getCurrentUser().first()
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        val ownerMember = ProjectMember(
            projectId = project.id,
            userId = currentUser.id,
            email = currentUser.email,
            displayName = currentUser.displayName ?: currentUser.email,
            role = ProjectRole.OWNER
        )
        val projectToSave = project.copy(
            createdAt = Date(),
            modifiedAt = Date(),
            syncStatus = SyncStatus.PENDING,
            ownerId = currentUser.id,
            members = (project.members + ownerMember).distinctBy { it.userId }
        )

        val entity = projectMapper.domainToEntity(projectToSave)
        projectDao.insertProject(entity)
        projectMemberDao.upsertAll(
            projectToSave.members.map { member ->
                projectMemberMapper.domainToEntity(member.copy(projectId = projectToSave.id))
            }
        )

        tryImmediateSync(projectToSave, isNewProject = true)
        return Result.success(projectToSave)
    }

    private suspend fun tryImmediateSync(project: Project, isNewProject: Boolean) {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                val localMembers = projectMemberDao.getMembersForProjectsList(listOf(project.id))
                    .map { projectMemberMapper.entityToDomain(it) }
                val dto = projectMapper.domainToDto(
                    project.copy(
                        ownerId = currentUser.id,
                        members = if (project.members.isNotEmpty()) project.members else localMembers
                    )
                ).copy(ownerId = currentUser.id)
                val projectResult = if (isNewProject) {
                    firebaseProjectSource.createProject(dto)
                } else {
                    firebaseProjectSource.updateProject(dto)
                }

                if (projectResult.isFailure) {
                    throw projectResult.exceptionOrNull() ?: IllegalStateException("Project sync failed")
                }

                val membersToSync = (localMembers + project.members).ifEmpty {
                    listOf(
                        ProjectMember(
                            projectId = project.id,
                            userId = currentUser.id,
                            email = currentUser.email,
                            displayName = currentUser.displayName ?: currentUser.email,
                            role = ProjectRole.OWNER
                        )
                    )
                }.distinctBy { it.userId }

                membersToSync.forEach { member ->
                    firebaseProjectSource.addProjectMember(
                        project.id,
                        projectMemberMapper.domainToDto(member.copy(projectId = project.id))
                    ).getOrThrow()
                }

                projectDao.updateSyncStatus(project.id, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed immediate sync", e)
        }
    }


    override suspend fun updateProject(project: Project): Result<Project> {
        val updatedProject = project.copy(
            modifiedAt = Date(),
            syncStatus = SyncStatus.PENDING
        )

        val entity = projectMapper.domainToEntity(updatedProject)
        projectDao.updateProject(entity)
        tryImmediateSync(updatedProject, isNewProject = false)

        return Result.success(updatedProject)
    }


    override suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            val userId = firebaseAuthSource.getCurrentUserId()

            val project = projectDao.getProjectById(projectId).firstOrNull()
            if (project == null) {
                return Result.failure(IllegalStateException("Project not found"))
            }

            if (userId != project.ownerId) {
                return Result.failure(IllegalStateException("Only the project owner can delete this project"))
            }


            // Then delete from Firebase
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                deleteProjectAndRelatedDataFromFirebase(projectId)
            } else {
                Log.w(TAG, "Current user missing while deleting project remotely; continuing with local delete.")
            }

            // First delete from Room
            projectDao.deleteProject(projectId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while deleting project", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteProjectAndRelatedDataFromFirebase(projectId: String) {
        try {
            // 1. First delete all tasks in the project
            firebaseProjectSource.deleteAllTasksForProject(projectId)
            // 2. Then delete all invitations for the project
            firebaseProjectSource.deleteAllInvitationsForProject(projectId)

            // 3. Then delete all members of the project
            firebaseProjectSource.deleteAllMembersForProject(projectId)

            // 4. Finally delete the project itself
            firebaseProjectSource.deleteProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete project and related data from Firebase", e)
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
                val members = projectMemberDao.getMembersForProjectsList(listOf(entity.id))
                    .map { projectMemberMapper.entityToDomain(it) }
                val dto = projectMapper.entityToDto(entity).copy(
                    ownerId = userId,
                    members = (listOf(userId) + members.map { it.userId }).distinct(),
                    memberIds = (listOf(userId) + members.map { it.userId }).distinct()
                )
                val result = if (entity.createdAt == entity.modifiedAt) {
                    firebaseProjectSource.createProject(dto)
                } else {
                    firebaseProjectSource.updateProject(dto)
                }

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
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val normalizedEmail = userEmail.trim().lowercase()
            val project = projectDao.getProjectById(projectId).firstOrNull()
                ?: return Result.failure(IllegalStateException("Project not found"))

            if (project.ownerId != currentUser.id) {
                return Result.failure(IllegalStateException("Only the project owner can invite members"))
            }
            if (normalizedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                return Result.failure(IllegalArgumentException("Enter a valid email address"))
            }
            if (normalizedEmail == currentUser.email.trim().lowercase()) {
                return Result.failure(IllegalArgumentException("You cannot invite yourself to your own project"))
            }

            val currentMembers = projectMemberDao.getMembersByProject(projectId).first()
            if (currentMembers.any { it.email.equals(normalizedEmail, ignoreCase = true) }) {
                return Result.failure(IllegalStateException("This user is already a project member"))
            }

            val existingPendingInvite = projectInvitationDao.getAllInvitations()
                .first()
                .map(projectInvitationMapper::entityToDomain)
                .any {
                    it.projectId == projectId &&
                        it.inviteeEmail.equals(normalizedEmail, ignoreCase = true) &&
                        it.status == InvitationStatus.PENDING
                }
            if (existingPendingInvite) {
                return Result.failure(IllegalStateException("A pending invitation already exists for this email"))
            }

            val invitation = ProjectInvitation(
                projectId = projectId,
                projectTitle = projectTitle.ifBlank { project.title },
                inviterId = currentUser.id,
                inviterName = currentUser.displayName ?: currentUser.email,
                inviteeId = "",
                inviteeEmail = normalizedEmail,
                createdAt = Date()
            )

            val invitationEntity = projectInvitationMapper.domainToEntity(invitation)
            projectInvitationDao.insertInvitation(invitationEntity)

            try {
                val invitationDto = projectInvitationMapper.domainToDto(invitation)
                firebaseProjectSource.createInvitation(invitationDto)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync invitation", e)
            }

            return Result.success(invitation)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getProjectInvitations(): Flow<List<ProjectInvitation>> {
        return projectInvitationDao.getAllInvitations().map { entities ->
            entities.map { projectInvitationMapper.entityToDomain(it) }
        }
    }

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> {
        try {
            val invitation = projectInvitationDao.getInvitationById(invitationId).first()
                ?: return Result.failure(IllegalStateException("Invitation not found"))
            val invitationDomain = projectInvitationMapper.entityToDomain(invitation)
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val updatedInvitation = invitationDomain.copy(
                inviteeId = currentUser.id,
                status = if (accept) InvitationStatus.ACCEPTED else InvitationStatus.REJECTED
            )

            val invitationEntity = projectInvitationMapper.domainToEntity(updatedInvitation)
            projectInvitationDao.updateInvitation(invitationEntity)

            if (accept) {
                val existingMembers = projectMemberDao.getMembersByProject(invitationDomain.projectId).first()
                val alreadyMember = existingMembers.any { it.userId == currentUser.id }

                if (!alreadyMember) {
                    val member = ProjectMember(
                        projectId = invitationDomain.projectId,
                        userId = currentUser.id,
                        email = currentUser.email,
                        displayName = currentUser.displayName ?: currentUser.email,
                        role = ProjectRole.MEMBER
                    )
                    projectMemberDao.insertMember(projectMemberMapper.domainToEntity(member))

                    try {
                        val memberDto = projectMemberMapper.domainToDto(member)
                        firebaseProjectSource.addProjectMember(invitationDomain.projectId, memberDto)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync member addition", e)
                    }
                }
            }

            try {
                val invitationDto = projectInvitationMapper.domainToDto(updatedInvitation)
                firebaseProjectSource.updateInvitation(invitationDto)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync invitation response", e)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in respondToInvitation", e)
            return Result.failure(e)
        }
    }

    override suspend fun getProjectMembers(projectId: String): Flow<List<ProjectMember>> {
        try {
            val firebaseResult = firebaseProjectSource.getProjectMembers(projectId)
            if (firebaseResult.isSuccess) {
                val firebaseMembers = firebaseResult.getOrNull().orEmpty()
                projectMemberDao.deleteMembersByProject(projectId)

                firebaseMembers.forEach { memberDto ->
                    val memberEntity = projectMemberMapper.dtoToEntity(memberDto)
                    projectMemberDao.insertMember(memberEntity)
                }

                return flow { emit(firebaseMembers.map { projectMemberMapper.dtoToDomain(it) }) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting members from Firebase", e)
        }

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
                firebaseProjectSource.removeProjectMember(projectId, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync member removal", e)
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

    override suspend fun inviteProjectMember(projectId: String, projectTitle: String, userEmail: String): Result<Unit> {
        // Reuse existing inviteUserToProject method but convert the result to Result<Unit>
        return inviteUserToProject(projectId, projectTitle, userEmail).map { Unit }
    }

    override suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit> {
        // Reuse existing removeMemberFromProject method
        return removeMemberFromProject(projectId, userId)
    }
}
