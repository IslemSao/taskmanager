// domain/usecase/InviteProjectMemberUseCase.kt
package com.example.taskmanager.domain.usecase.project

import android.util.Log
import com.example.taskmanager.domain.model.ProjectInvitation
import com.example.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class InviteProjectMemberUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String, projectTitle: String, userEmail: String): Result<Unit> {
        if (userEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        
        return projectRepository.inviteProjectMember(projectId, projectTitle, userEmail)
    }
}
