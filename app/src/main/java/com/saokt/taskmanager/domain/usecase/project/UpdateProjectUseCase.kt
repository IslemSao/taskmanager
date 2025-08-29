package com.saokt.taskmanager.domain.usecase.project

import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class UpdateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(project: Project): Result<Project> {
        return projectRepository.updateProject(project)
    }
}
