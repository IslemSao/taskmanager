// Updated TaskRepositoryImpl with Firestore sync
package com.saokt.taskmanager.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.mapper.TaskMapper
import com.saokt.taskmanager.data.remote.firebase.FirebaseTaskSource
import com.saokt.taskmanager.domain.model.SyncStatus
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val userRepository: UserRepository, // Add this dependency
    private val widgetRefresher: com.saokt.taskmanager.widget.WidgetRefresher,
    @ApplicationContext private val context: Context
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

    override suspend fun getTasksByProjectFromFirebase(projectId: String): Result<List<Task>> {
        return try {
            Log.d("ProjectTasksDebug", "TaskRepositoryImpl: getTasksByProjectFromFirebase called for projectId: $projectId")
            val result = firebaseTaskSource.getTasksByProject(projectId)
            
            if (result.isSuccess) {
                val tasks = result.getOrNull()?.map { dto ->
                    taskMapper.dtoToDomain(dto)
                } ?: emptyList()
                
                Log.d("ProjectTasksDebug", "TaskRepositoryImpl: Successfully fetched ${tasks.size} tasks from Firebase")
                tasks.forEach { task ->
                    Log.d("ProjectTasksDebug", "TaskRepositoryImpl: Task from Firebase: ${task.title}, userId: ${task.userId}, createdBy: ${task.createdBy}")
                }
                
                Result.success(tasks)
            } else {
                Log.e("ProjectTasksDebug", "TaskRepositoryImpl: Failed to fetch tasks from Firebase: ${result.exceptionOrNull()?.message}")
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch tasks"))
            }
        } catch (e: Exception) {
            Log.e("ProjectTasksDebug", "TaskRepositoryImpl: Exception in getTasksByProjectFromFirebase", e)
            Result.failure(e)
        }
    }

    override suspend fun createTask(task: Task): Result<Task> {
        Log.d("TaskCreationDebug", "Repository: createTask called with $task")
        Log.d("TaskCreationDebug", "Repository: calling remoteDataSource.createTask")
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

            // Refresh widget
            widgetRefresher.refreshTopTasksWidget()

            return Result.success(taskToSave)
        } catch (e: Exception) {
            Log.e("TaskCreationDebug", "Repository: Error creating task", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        Log.d("TaskCreationDebug", "Repository: updateTask called with $task")
        Log.d("TaskCreationDebug", "Repository: calling remoteDataSource.updateTask")
        try {
            val updatedTask = task.copy(
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )

            val entity = taskMapper.domainToEntity(updatedTask)
            taskDao.updateTask(entity)

            // Try to immediately sync with Firestore
            tryImmediateSync(updatedTask)

            // Refresh widget
            widgetRefresher.refreshTopTasksWidget()

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

            // Refresh widget
            widgetRefresher.refreshTopTasksWidget()

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

            // Refresh widget
            widgetRefresher.refreshTopTasksWidget()

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

            // Refresh widget
            widgetRefresher.refreshTopTasksWidget()

            return Result.success(completedTask)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun tryImmediateSync(task: Task) {
        // Skip sync if no network connection
        if (!isNetworkAvailable()) {
            Log.i("TaskSync", "No network connection - task will sync when connection is restored")
            return
        }

        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                // Set userId to the assigned user's ID, or current user if not assigned
                val userId = task.assignedTo ?: currentUser.id
                val dto = taskMapper.domainToDto(task).copy(
                    userId = userId,
                    createdBy = if (task.createdBy.isBlank()) currentUser.id else task.createdBy
                )
                Log.d("TaskCreationDebug", "tryImmediateSync: currentUser.id: ${currentUser.id}")
                Log.d("TaskCreationDebug", "tryImmediateSync: assignedTo: ${task.assignedTo}")
                Log.d("TaskCreationDebug", "tryImmediateSync: userId: $userId")
                Log.d("TaskCreationDebug", "tryImmediateSync: dto: $dto")
                // Check if this is a new task (no sync status or PENDING) or existing task
                val existingTask = taskDao.getTaskById(task.id).firstOrNull()
                val isNewTask = existingTask == null || existingTask.syncStatus == SyncStatus.PENDING
                Log.d("TaskCreationDebug", "tryImmediateSync: isNewTask: $isNewTask")
                val result = if (isNewTask) {
                    Log.d("TaskCreationDebug", "tryImmediateSync: Calling createTask")
                    firebaseTaskSource.createTask(dto)
                } else {
                    Log.d("TaskCreationDebug", "tryImmediateSync: Calling updateTask")
                    firebaseTaskSource.updateTask(dto)
                }
                if (result.isSuccess) {
                    Log.d("TaskCreationDebug", "tryImmediateSync: Sync successful")
                    // If successful, update the sync status in Room
                    taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED)
                } else {
                    Log.w("TaskCreationDebug", "tryImmediateSync: Sync failed: ${result.exceptionOrNull()?.message}")
                    // Keep the task as PENDING for background sync to retry later
                }
            } else {
                Log.e("TaskCreationDebug", "tryImmediateSync: No current user")
            }
        } catch (e: Exception) {
            // Network or other errors should not crash the app
            // The task is already saved locally with PENDING status for background sync
            Log.w("TaskSync", "Immediate sync failed (will retry in background): ${e.message}")
            when (e) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException -> {
                    Log.i("TaskSync", "Network connectivity issue - task will sync when connection is restored")
                }
                else -> {
                    Log.e("TaskSync", "Unexpected sync error", e)
                }
            }
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
