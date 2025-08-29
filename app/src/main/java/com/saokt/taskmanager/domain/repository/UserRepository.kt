// domain/repository/UserRepository.kt
package com.saokt.taskmanager.domain.repository

import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    fun isUserAuthenticated(): Boolean
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    // NEW listener flows
    fun listenToRemoteProjects(): Flow<Result<Pair<List<ProjectDto>, List<ProjectMemberDto>>>>
    fun listenToRemoteTasks(): Flow<Result<List<TaskDto>>>
    fun listenToRemoteInvitations(): Flow<Result<List<ProjectInvitationDto>>>

    // NEW sync methods
    suspend fun syncRemoteProjectsToLocal(projectDtos: List<ProjectDto>)
    suspend fun syncRemoteTasksToLocal(taskDtos: List<TaskDto>)
    suspend fun syncRemoteInvitationsToLocal(projectInvitationDtos: List<ProjectInvitationDto>)
    suspend fun syncRemoteMembersToLocal(projectMemberDtos: List<ProjectMemberDto>)
    suspend fun updateUser(user: User): Result<Unit>
}
