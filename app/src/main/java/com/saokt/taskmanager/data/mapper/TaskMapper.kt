package com.saokt.taskmanager.data.mapper

import com.saokt.taskmanager.data.local.entity.TaskEntity
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.Task
import javax.inject.Inject

class TaskMapper @Inject constructor() {

    fun domainToEntity(task: Task): TaskEntity {
        return TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            dueDate = task.dueDate,
            completed = task.completed,
            priority = task.priority,
            projectId = task.projectId,
            labels = task.labels,
            subtasks = task.subtasks,
            createdAt = task.createdAt,
            modifiedAt = task.modifiedAt,
            syncStatus = task.syncStatus,
            userId = task.userId,
            createdBy = task.createdBy,
            assignedTo = task.assignedTo,
            assignedBy = task.assignedBy
        )
    }

    fun entityToDomain(entity: TaskEntity): Task {
        return Task(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            dueDate = entity.dueDate,
            completed = entity.completed,
            priority = entity.priority,
            projectId = entity.projectId,
            labels = entity.labels,
            subtasks = entity.subtasks,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
            syncStatus = entity.syncStatus,
            userId = entity.userId ?: "",
            createdBy = entity.createdBy,
            assignedTo = entity.assignedTo,
            assignedAt = null, // Set if you store it
            assignedBy = entity.assignedBy
        )
    }

    fun domainToDto(task: Task): TaskDto {
        return TaskDto(
            id = task.id,
            title = task.title,
            description = task.description,
            dueDate = task.dueDate,
            completed = task.completed,
            priority = task.priority.name,
            projectId = task.projectId,
            labels = task.labels,
            subtasks = task.subtasks.map {
                mapOf("id" to it.id, "title" to it.title, "isCompleted" to it.isCompleted)
            },
            createdAt = task.createdAt,
            modifiedAt = task.modifiedAt,
            userId = task.userId,
            createdBy = task.createdBy,
            assignedTo = task.assignedTo,
            assignedAt = task.assignedAt,
            assignedBy = task.assignedBy
        )
    }

    fun entityToDto(entity: TaskEntity): TaskDto {
        return TaskDto(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            dueDate = entity.dueDate,
            completed = entity.completed,
            priority = entity.priority.name,
            projectId = entity.projectId,
            labels = entity.labels,
            subtasks = entity.subtasks.map {
                mapOf("id" to it.id, "title" to it.title, "isCompleted" to it.isCompleted)
            },
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
            userId = entity.userId ?: "",
            createdBy = entity.createdBy,
            assignedTo = entity.assignedTo,
            assignedAt = null, // TaskEntity doesn't store assignedAt
            assignedBy = entity.assignedBy
        )
    }

    fun dtoToDomain(dto: TaskDto): Task {
        return Task(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            dueDate = dto.dueDate,
            completed = dto.completed,
            priority = try {
                com.saokt.taskmanager.domain.model.Priority.valueOf(dto.priority.uppercase())
            } catch (e: Exception) {
                com.saokt.taskmanager.domain.model.Priority.MEDIUM
            },
            projectId = dto.projectId,
            labels = dto.labels,
            subtasks = dto.subtasks.mapNotNull { subtaskMap ->
                try {
                    com.saokt.taskmanager.domain.model.Subtask(
                        id = subtaskMap["id"] as? String ?: return@mapNotNull null,
                        title = subtaskMap["title"] as? String ?: "",
                        isCompleted = subtaskMap["isCompleted"] as? Boolean ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            },
            createdAt = dto.createdAt,
            modifiedAt = dto.modifiedAt,
            syncStatus = com.saokt.taskmanager.domain.model.SyncStatus.SYNCED,
            userId = dto.userId,
            createdBy = dto.createdBy,
            assignedTo = dto.assignedTo,
            assignedAt = dto.assignedAt,
            assignedBy = dto.assignedBy
        )
    }
}
