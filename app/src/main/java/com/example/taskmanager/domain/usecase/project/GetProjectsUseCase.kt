// Create similar GetLocalProjectsUseCase using ProjectDao
package com.example.taskmanager.domain.usecase.project

import com.example.taskmanager.data.local.dao.ProjectDao
import com.example.taskmanager.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val projectDao: ProjectDao
) {
    operator fun invoke(): Flow<List<Project>> {
        return projectDao.getAllProjectsFlow().map { entities ->
            entities.map { entity ->
                // Your mapping logic from ProjectEntity to Project domain model
                Project(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    ownerId = entity.ownerId,
                    color = entity.color,
                    createdAt = entity.createdAt,
                    modifiedAt = entity.modifiedAt,
                    syncStatus = entity.syncStatus,
                )
            }
        }
    }
}