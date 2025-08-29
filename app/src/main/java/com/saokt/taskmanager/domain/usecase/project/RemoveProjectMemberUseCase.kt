// domain/usecase/RemoveProjectMemberUseCase.kt
package com.saokt.taskmanager.domain.usecase.project

import com.saokt.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class RemoveProjectMemberUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String, userId: String): Result<Unit> {
        if (projectId.isBlank() || userId.isBlank()) {
            return Result.failure(IllegalArgumentException("Project ID and User ID cannot be empty"))
        }
        
        return projectRepository.removeProjectMember(projectId, userId)
    }
}
