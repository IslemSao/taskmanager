package com.saokt.taskmanager.data.mapper

import com.saokt.taskmanager.data.local.entity.TaskEntity
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.TaskType
import com.saokt.taskmanager.domain.model.canonicalTaskStatus
import com.saokt.taskmanager.domain.model.canonicalized
import javax.inject.Inject

class TaskMapper @Inject constructor() {

    fun domainToEntity(task: Task): TaskEntity {
        val normalizedTask = task.canonicalized()
        return TaskEntity(
            id = normalizedTask.id,
            title = normalizedTask.title,
            description = normalizedTask.description,
            startDate = normalizedTask.startDate,
            dueDate = normalizedTask.dueDate,
            completed = normalizedTask.completed,
            status = normalizedTask.status,
            type = normalizedTask.type,
            priority = normalizedTask.priority,
            projectId = normalizedTask.projectId,
            labels = normalizedTask.labels,
            subtasks = normalizedTask.subtasks,
            createdAt = normalizedTask.createdAt,
            modifiedAt = normalizedTask.modifiedAt,
            syncStatus = normalizedTask.syncStatus,
            userId = normalizedTask.userId,
            createdBy = normalizedTask.createdBy,
            assignedTo = normalizedTask.assignedTo,
            assignedBy = normalizedTask.assignedBy,
            visibleToUserIds = normalizedTask.visibleToUserIds
        )
    }

    fun entityToDomain(entity: TaskEntity): Task {
        return Task(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            startDate = entity.startDate,
            dueDate = entity.dueDate,
            completed = entity.completed,
            status = canonicalTaskStatus(entity.status, entity.completed),
            type = entity.type,
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
            assignedBy = entity.assignedBy,
            visibleToUserIds = entity.visibleToUserIds
        ).canonicalized()
    }

    fun domainToDto(task: Task): TaskDto {
        val normalizedTask = task.canonicalized()
        return TaskDto(
            id = normalizedTask.id,
            title = normalizedTask.title,
            description = normalizedTask.description,
            startDate = normalizedTask.startDate,
            dueDate = normalizedTask.dueDate,
            completed = normalizedTask.completed,
            status = normalizedTask.status.name,
            type = normalizedTask.type.name,
            priority = normalizedTask.priority.name,
            projectId = normalizedTask.projectId,
            labels = normalizedTask.labels,
            subtasks = normalizedTask.subtasks.map {
                mapOf("id" to it.id, "title" to it.title, "isCompleted" to it.isCompleted)
            },
            createdAt = normalizedTask.createdAt,
            modifiedAt = normalizedTask.modifiedAt,
            userId = normalizedTask.userId,
            createdBy = normalizedTask.createdBy,
            assignedTo = normalizedTask.assignedTo,
            assignedAt = normalizedTask.assignedAt,
            assignedBy = normalizedTask.assignedBy,
            visibleToUserIds = normalizedTask.visibleToUserIds
        )
    }

    fun entityToDto(entity: TaskEntity): TaskDto {
        val normalizedStatus = canonicalTaskStatus(entity.status, entity.completed)
        return TaskDto(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            startDate = entity.startDate,
            dueDate = entity.dueDate,
            completed = entity.completed,
            status = normalizedStatus.name,
            type = entity.type.name,
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
            assignedBy = entity.assignedBy,
            visibleToUserIds = entity.visibleToUserIds
        )
    }

    fun dtoToDomain(dto: TaskDto): Task {
        return Task(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            startDate = dto.startDate,
            dueDate = dto.dueDate,
            completed = dto.completed,
            status = try {
                dto.status?.let { TaskStatus.valueOf(it.uppercase()) }
            } catch (_: Exception) {
                null
            } ?: canonicalTaskStatus(null, dto.completed),
            type = try {
                dto.type?.let { TaskType.valueOf(it.uppercase()) }
            } catch (_: Exception) {
                null
            } ?: TaskType.TASK,
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
            assignedBy = dto.assignedBy,
            visibleToUserIds = dto.visibleToUserIds
        ).canonicalized()
    }
}
