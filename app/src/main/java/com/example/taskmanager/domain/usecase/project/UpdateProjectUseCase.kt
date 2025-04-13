package com.example.taskmanager.domain.usecase.project

import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class UpdateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(project: Project): Result<Project> {
        // Validate that the project title is not empty
        if (project.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Project title cannot be empty"))
        }
        // Call the repository to update the project details
        return projectRepository.updateProject(project)
    }
}
