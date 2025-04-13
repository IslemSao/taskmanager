// domain/usecase/InviteProjectMemberUseCase.kt
package com.example.taskmanager.domain.usecase.project

import android.util.Log
import com.example.taskmanager.domain.model.ProjectInvitation
import com.example.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class InviteProjectMemberUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String, projectTitle : String ,  userEmail: String): Result<ProjectInvitation> {
        if (userEmail.isBlank()) {
            return Result.failure(Exception("Email cannot be empty"))
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            return Result.failure(Exception("Invalid email format"))
        }

        Log.d("invitation", "usecase")
        return projectRepository.inviteUserToProject(projectId, projectTitle ,  userEmail)
    }
}
