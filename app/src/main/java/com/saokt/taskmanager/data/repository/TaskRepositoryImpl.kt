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
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.canonicalized
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
    private val userRepository: UserRepository,
    private val widgetRefresher: com.saokt.taskmanager.widget.WidgetRefresher,
    @ApplicationContext private val context: Context
) : TaskRepository {
    companion object {
        private const val TAG = "TaskRepository"
    }

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { taskMapper.entityToDomain(it) }
        }
    }

    override fun getTaskById(taskId: String): Flow<Task?> {
        return taskDao.getTaskById(taskId).map { entity -> entity?.let(taskMapper::entityToDomain) }
    }

    override suspend fun fetchTaskByIdRemote(taskId: String): Result<Task?> {
        return try {
            val dtoResult = firebaseTaskSource.getTaskById(taskId)
            if (dtoResult.isSuccess) {
                val dto = dtoResult.getOrNull()!!
                val domain = taskMapper.dtoToDomain(dto).copy(syncStatus = SyncStatus.SYNCED)
                taskDao.insertTask(taskMapper.domainToEntity(domain))
                Result.success(domain)
            } else {
                Result.failure(dtoResult.exceptionOrNull() ?: Exception("Remote fetch failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch task from Firebase", e)
            Result.failure(e)
        }
    }

    override fun getTasksByProject(projectId: String): Flow<List<Task>> {
        return taskDao.getTasksByProject(projectId).map { entities ->
            entities.map { taskMapper.entityToDomain(it) }
        }
    }

    override suspend fun getTasksByProjectFromFirebase(projectId: String): Result<List<Task>> {
        return try {
            val result = firebaseTaskSource.getTasksByProject(projectId)
            if (result.isSuccess) {
                val tasks = result.getOrNull().orEmpty().map(taskMapper::dtoToDomain)
                taskDao.insertTasks(tasks.map { taskMapper.domainToEntity(it.copy(syncStatus = SyncStatus.SYNCED)) })
                Result.success(tasks)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch tasks"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch project tasks from Firebase", e)
            Result.failure(e)
        }
    }

    override suspend fun createTask(task: Task): Result<Task> {
        try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val taskToSave = task.copy(
                createdAt = Date(),
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING,
                userId = currentUser.id,
                createdBy = task.createdBy.ifBlank { currentUser.id },
                visibleToUserIds = buildVisibleUsers(
                    creatorId = task.createdBy.ifBlank { currentUser.id },
                    assignedTo = task.assignedTo
                )
            ).canonicalized()

            taskDao.insertTask(taskMapper.domainToEntity(taskToSave))

            tryImmediateSync(taskToSave, isNewTask = true)
            widgetRefresher.refreshTopTasksWidget()

            return Result.success(taskToSave)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create task", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val updatedTask = task.copy(
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING,
                userId = task.userId.ifBlank { currentUser.id },
                createdBy = task.createdBy.ifBlank { currentUser.id },
                visibleToUserIds = buildVisibleUsers(
                    creatorId = task.createdBy.ifBlank { currentUser.id },
                    assignedTo = task.assignedTo
                )
            ).canonicalized()

            taskDao.updateTask(taskMapper.domainToEntity(updatedTask))

            tryImmediateSync(updatedTask, isNewTask = false)
            widgetRefresher.refreshTopTasksWidget()

            return Result.success(updatedTask)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update task", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        try {
            taskDao.deleteTask(taskId)

            try {
                firebaseTaskSource.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task from Firestore", e)
            }

            widgetRefresher.refreshTopTasksWidget()

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun toggleTaskCompletion(task: Task): Result<Task> {
        try {
            val newCompletionStatus = !task.completed
            val newStatus = if (newCompletionStatus) TaskStatus.DONE else TaskStatus.TODO
            val modifiedAt = Date()

            val rowsAffected = taskDao.updateTaskCompletion(
                taskId = task.id,
                isCompleted = newCompletionStatus,
                status = newStatus.name,
                modifiedAt = modifiedAt,
                syncStatus = SyncStatus.PENDING
            )

            if (rowsAffected <= 0) {
                return Result.failure(Exception("Failed to update task in database"))
            }

            val updatedTask = task.copy(
                completed = newCompletionStatus,
                status = newStatus,
                modifiedAt = modifiedAt,
                syncStatus = SyncStatus.PENDING
            ).canonicalized()

            tryImmediateSync(updatedTask, isNewTask = false)
            widgetRefresher.refreshTopTasksWidget()

            return Result.success(updatedTask)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle task completion", e)
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
                status = TaskStatus.DONE,
                modifiedAt = Date(),
                syncStatus = SyncStatus.PENDING
            ).canonicalized()

            val entity = taskMapper.domainToEntity(completedTask)
            taskDao.updateTask(entity)

            tryImmediateSync(completedTask, isNewTask = false)
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

    private suspend fun tryImmediateSync(task: Task, isNewTask: Boolean) {
        if (!isNetworkAvailable()) {
            return
        }

        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser == null) {
                return
            }

            val syncResult = syncTaskToRemote(task, currentUser.id, preferCreate = isNewTask)
            if (syncResult.isFailure) {
                Log.w(TAG, "Immediate task sync failed: ${syncResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync failed; task will retry later", e)
            when (e) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException -> Unit
                else -> Log.e(TAG, "Unexpected task sync error", e)
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
                val result = syncTaskToRemote(
                    task = task,
                    currentUserId = currentUser.id,
                    preferCreate = task.createdAt == task.modifiedAt
                )

                if (result.isSuccess) {
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

    private suspend fun syncTaskToRemote(
        task: Task,
        currentUserId: String,
        preferCreate: Boolean
    ): Result<Task> {
        val normalizedTask = task.canonicalized()
        val dto = taskMapper.domainToDto(normalizedTask).copy(
            userId = normalizedTask.userId.ifBlank { currentUserId },
            createdBy = normalizedTask.createdBy.ifBlank { currentUserId },
            visibleToUserIds = normalizedTask.visibleToUserIds.ifEmpty {
                buildVisibleUsers(
                    creatorId = normalizedTask.createdBy.ifBlank { currentUserId },
                    assignedTo = normalizedTask.assignedTo
                )
            }
        )

        val primaryResult = if (preferCreate) {
            firebaseTaskSource.createTask(dto)
        } else {
            firebaseTaskSource.updateTask(dto)
        }

        val finalResult = when {
            primaryResult.isSuccess -> primaryResult
            !preferCreate && isTaskNotFound(primaryResult.exceptionOrNull()) -> firebaseTaskSource.createTask(dto)
            else -> primaryResult
        }

        if (finalResult.isFailure) {
            return Result.failure(finalResult.exceptionOrNull() ?: Exception("Task sync failed"))
        }

        val syncedTask = taskMapper.dtoToDomain(finalResult.getOrThrow())
            .copy(syncStatus = SyncStatus.SYNCED)
            .canonicalized()
        taskDao.insertTask(taskMapper.domainToEntity(syncedTask))
        taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED)
        return Result.success(syncedTask)
    }

    private fun buildVisibleUsers(creatorId: String, assignedTo: String?): List<String> =
        listOfNotNull(creatorId, assignedTo).distinct()

    private fun isTaskNotFound(error: Throwable?): Boolean =
        error is IllegalStateException && error.message == "Task not found"
}
