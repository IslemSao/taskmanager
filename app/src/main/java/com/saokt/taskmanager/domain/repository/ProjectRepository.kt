package com.saokt.taskmanager.domain.repository

import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectInvitation
import com.saokt.taskmanager.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(projectId: String): Flow<Project?>
    suspend fun createProject(project: Project): Result<Project>
    suspend fun updateProject(project: Project): Result<Project>
    suspend fun deleteProject(projectId: String): Result<Unit>
    suspend fun completeProject(projectId: String): Result<Project>
    suspend fun syncPendingProjects(): Result<Int>
    suspend fun inviteUserToProject(projectId: String, projectTitle: String  ,userEmail: String): Result<ProjectInvitation>
    suspend fun getProjectInvitations(): Flow<List<ProjectInvitation>>
    suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit>
    suspend fun getProjectMembers(projectId: String): Flow<List<ProjectMember>>
    suspend fun removeMemberFromProject(projectId: String, userId: String): Result<Unit>

    suspend fun getProjectMembers(): Flow<List<ProjectMember>>

    suspend fun inviteProjectMember(projectId: String, projectTitle: String, userEmail: String): Result<Unit>
    suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit>
}
