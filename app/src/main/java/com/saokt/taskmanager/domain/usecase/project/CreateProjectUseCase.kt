package com.saokt.taskmanager.domain.usecase.project

import android.util.Log
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.repository.ProjectRepository
import javax.inject.Inject

class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(project: Project): Result<Project> {
        if (project.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Project title cannot be empty"))
        }
        Log.d("addProjectDebug", "createProject in usecase: $project")
        return projectRepository.createProject(project)
    }
}
