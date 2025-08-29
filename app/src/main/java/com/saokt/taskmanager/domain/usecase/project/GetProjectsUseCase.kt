// Create similar GetLocalProjectsUseCase using ProjectDao
package com.saokt.taskmanager.domain.usecase.project

import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> {
        return projectRepository.getAllProjects()
    }
}