package com.example.taskmanager.data.mapper

import com.example.taskmanager.data.local.entity.TaskEntity
import com.example.taskmanager.data.remote.dto.TaskDto
import com.example.taskmanager.domain.model.Task
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
            syncStatus = task.syncStatus
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
            syncStatus = entity.syncStatus
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
            userId = "" // Will be set by FirebaseTaskSource
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
            userId = "" // Will be set by FirebaseTaskSource
        )
    }
}
