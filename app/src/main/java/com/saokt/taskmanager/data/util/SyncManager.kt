// SyncManager.kt
package com.saokt.taskmanager.data.util

import android.content.Context
import androidx.work.*
import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    // Call this method to immediately trigger a sync
    fun triggerImmediateSync() {
        syncScope.launch {
            println("Triggering immediate sync...")
            try {
                val projectResult = projectRepository.syncPendingProjects()
                val taskResult = taskRepository.syncPendingTasks()

                println("Sync complete - Projects: ${projectResult.getOrNull() ?: 0}, Tasks: ${taskResult.getOrNull() ?: 0}")
            } catch (e: Exception) {
                println("Sync failed: ${e.message}")
            }
        }
    }

    // Schedule background periodic sync
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync_data_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        println("Scheduled periodic sync (every 15 minutes)")
    }

    // For immediate one-time sync (useful when network becomes available)
    fun scheduleOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueue(syncRequest)

        println("Scheduled one-time sync")
    }

    class SyncWorker(
        appContext: Context,
        params: WorkerParameters
    ) : CoroutineWorker(appContext, params) {

        @Inject lateinit var taskRepository: TaskRepository
        @Inject lateinit var projectRepository: ProjectRepository

        override suspend fun doWork(): Result {
            try {
                println("SyncWorker: Starting sync...")
                projectRepository.syncPendingProjects()
                taskRepository.syncPendingTasks()
                println("SyncWorker: Sync completed")
                return Result.success()
            } catch (e: Exception) {
                println("SyncWorker: Sync failed: ${e.message}")
                return Result.retry()
            }
        }
    }
}
