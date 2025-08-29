package com.saokt.taskmanager.domain.usecase.project

import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.ProjectMember
import com.saokt.taskmanager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectMembersUseCase @Inject constructor(
    val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): Flow<List<ProjectMember>> {
        return projectRepository.getProjectMembers(projectId)
    }
}