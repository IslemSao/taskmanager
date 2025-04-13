package com.example.taskmanager.domain.usecase.project

import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): Result<Unit> {
        return projectRepository.deleteProject(projectId)
    }
}
