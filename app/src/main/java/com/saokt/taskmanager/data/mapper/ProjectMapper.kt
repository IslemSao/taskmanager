package com.saokt.taskmanager.data.mapper

import com.saokt.taskmanager.data.local.entity.ProjectEntity
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.domain.model.Project
import javax.inject.Inject

class ProjectMapper @Inject constructor() {

    fun domainToEntity(project: Project): ProjectEntity {
        return ProjectEntity(
            id = project.id,
            title = project.title,
            description = project.description,
            color = project.color,
            startDate = project.startDate,
            dueDate = project.dueDate,
            isCompleted = project.isCompleted,
            createdAt = project.createdAt,
            modifiedAt = project.modifiedAt,
            syncStatus = project.syncStatus,
            ownerId = project.ownerId
        )
    }

    fun entityToDomain(entity: ProjectEntity): Project {
        return Project(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            color = entity.color,
            startDate = entity.startDate,
            dueDate = entity.dueDate,
            isCompleted = entity.isCompleted,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
            syncStatus = entity.syncStatus,
            ownerId = entity.ownerId
        )
    }

    fun domainToDto(project: Project): ProjectDto {
        return ProjectDto(
            id = project.id,
            title = project.title,
            description = project.description,
            color = project.color,
            startDate = project.startDate,
            dueDate = project.dueDate,
            completed = project.isCompleted,
            createdAt = project.createdAt,
            modifiedAt = project.modifiedAt,
            ownerId = project.ownerId
        )
    }

    fun entityToDto(entity: ProjectEntity): ProjectDto {
        return ProjectDto(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            color = entity.color,
            startDate = entity.startDate,
            dueDate = entity.dueDate,
            completed = entity.isCompleted,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
            ownerId = entity.ownerId
        )
    }
}
