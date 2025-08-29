package com.saokt.taskmanager.domain.usecase.project

import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.repository.ProjectRepository
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
