package com.example.taskmanager.domain.repository

import com.example.taskmanager.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(taskId: String): Flow<Task?>
    fun getTasksByProject(projectId: String): Flow<List<Task>>
    suspend fun createTask(task: Task): Result<Task>
    suspend fun updateTask(task: Task): Result<Task>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun completeTask(taskId: String): Result<Task>
    suspend fun syncPendingTasks(): Result<Int> // Returns number of synced tasks
    suspend fun toggleTaskcomplition(task: Task): Result<Task>
}
