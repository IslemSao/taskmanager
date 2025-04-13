package com.example.taskmanager.domain.usecase.project

import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectByIdUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(projectId: String): Flow<Project?> {
        // Retrieve a project by its id from the repository
        return projectRepository.getProjectById(projectId)
    }
}
