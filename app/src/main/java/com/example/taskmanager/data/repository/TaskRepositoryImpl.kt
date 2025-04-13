// Updated TaskRepositoryImpl with Firestore sync
package com.example.taskmanager.data.repository

import android.util.Log
import com.example.taskmanager.data.local.dao.TaskDao
import com.example.taskmanager.data.mapper.TaskMapper
import com.example.taskmanager.data.remote.firebase.FirebaseTaskSource
import com.example.taskmanager.domain.model.SyncStatus
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.domain.repository.TaskRepository
import com.example.taskmanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val firebaseTaskSource: FirebaseTaskSource,
    private val taskMapper: TaskMapper,
    private val userRepository: UserRepository // Add this dependency
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { taskMapper.entityToDomain(it) }
        }
    }

    override fun getTaskById(taskId: String): Flow<Task?> {
        return taskDao.getTaskById(taskId).map { entity ->
            entity?.let { taskMapper.entityToDomain(it) }
        }
    }

    override fun getTasksByProject(projectId: String): Flow<List<Task>> {
        return taskDao.getTasksByProject(projectId).map { entities ->
            entities.map { taskMapper.entityToDomain(it) }
        }
    }

    override suspend fun createTask(task: Task): Result<Task> {
        try {
            // First save to local database
            val taskToSave = task.copy(
                createdAt = Date(),
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )

            val entity = taskMapper.domainToEntity(taskToSave)
            taskDao.insertTask(entity)

            // Then try to immediately sync with Firestore
            tryImmediateSync(taskToSave)

            return Result.success(taskToSave)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        try {
            val updatedTask = task.copy(
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )

            val entity = taskMapper.domainToEntity(updatedTask)
            taskDao.updateTask(entity)

            // Try to immediately sync with Firestore
            tryImmediateSync(updatedTask)

            return Result.success(updatedTask)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        try {
            // First delete from local database
            Log.d("bombardiro" , "DeleteTaskRepo: $taskId")
            taskDao.deleteTask(taskId)

            // Then try to delete from Firestore
            // For proper deletion sync, you may need to mark the task as "deleted" rather than actually deleting it
            // That would require schema changes
            try {
                Log.d("bombardiro" , "DeleteTaskRepo Firebase: $taskId")
                firebaseTaskSource.deleteTask(taskId)
            } catch (e: Exception) {
                // Log but don't fail if Firestore deletion fails
                println("Failed to delete task from Firestore: ${e.message}")
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun toggleTaskcomplition(task: Task): Result<Task> {
        try {
            Log.d("TaskRepo", "Toggling completion for task: ${task.id}, current status: ${task.completed}")

            val newCompletionStatus = !task.completed
            val modifiedAt = Date()

            Log.d("TaskRepo", "New completion status will be: $newCompletionStatus")

            // Use the explicit update query
            val rowsAffected = taskDao.updateTaskCompletion(
                taskId = task.id,
                isCompleted = newCompletionStatus,
                modifiedAt = modifiedAt,
                syncStatus = SyncStatus.PENDING
            )

            Log.d("TaskRepo", "Database updated: $rowsAffected rows affected")

            if (rowsAffected <= 0) {
                Log.e("TaskRepo", "No rows updated in database!")
                return Result.failure(Exception("Failed to update task in database"))
            }

            val updatedTask = task.copy(
                completed = newCompletionStatus,
                modifiedAt = modifiedAt,
                syncStatus = SyncStatus.PENDING
            )

            // Try to immediately sync with Firestore
            tryImmediateSync(updatedTask)

            return Result.success(updatedTask)
        } catch (e: Exception) {
            Log.e("TaskRepo", "Error toggling task completion", e)
            return Result.failure(e)
        }
    }

    override suspend fun completeTask(taskId: String): Result<Task> {
        try {
            val taskFlow = taskDao.getTaskById(taskId)
            val taskEntity = taskFlow.firstOrNull() ?: return Result.failure(
                IllegalStateException("Task not found")
            )

            val task = taskMapper.entityToDomain(taskEntity)

            val completedTask = task.copy(
                completed = true,
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )

            val entity = taskMapper.domainToEntity(completedTask)
            taskDao.updateTask(entity)

            // Try to immediately sync with Firestore
            tryImmediateSync(completedTask)

            return Result.success(completedTask)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun tryImmediateSync(task: Task) {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                val dto = taskMapper.domainToDto(task).copy(userId = currentUser.id)
                firebaseTaskSource.updateTask(dto)
                // If successful, update the sync status in Room
                taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            // Log the error but don't fail - the background sync will handle it later
            println("Failed immediate sync: ${e.message}")
        }
    }

    override suspend fun syncPendingTasks(): Result<Int> {
        try {
            val pendingTasks = taskDao.getTasksBySyncStatus(SyncStatus.PENDING)
            var syncedCount = 0

            // Get the current user to associate tasks with
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("User not authenticated"))

            pendingTasks.forEach { entity ->
                val task = taskMapper.entityToDomain(entity)
                val dto = taskMapper.domainToDto(task).copy(userId = currentUser.id)

                val result = firebaseTaskSource.updateTask(dto)

                if (result.isSuccess) {
                    taskDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    syncedCount++
                } else {
                    taskDao.updateSyncStatus(entity.id, SyncStatus.SYNC_FAILED)
                }
            }

            return Result.success(syncedCount)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
