package com.example.taskmanager.domain.usecase.project

import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectMembersUseCase @Inject constructor(
    val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(): Flow<List<ProjectMember>> {
        return projectRepository.getProjectMembers()
    }
}