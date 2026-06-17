// SyncManager.kt
package com.saokt.taskmanager.data.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
    companion object {
        private const val TAG = "SyncManager"
    }

    private val syncScope = CoroutineScope(Dispatchers.IO)

    // Call this method to immediately trigger a sync
    fun triggerImmediateSync() {
        syncScope.launch {
            Log.d(TAG, "Triggering immediate sync")
            try {
                val projectResult = projectRepository.syncPendingProjects()
                val taskResult = taskRepository.syncPendingTasks()

                Log.d(
                    TAG,
                    "Sync complete - Projects: ${projectResult.getOrNull() ?: 0}, Tasks: ${taskResult.getOrNull() ?: 0}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
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

        Log.d(TAG, "Scheduled periodic sync")
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

        Log.d(TAG, "Scheduled one-time sync")
    }
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting sync")
            projectRepository.syncPendingProjects()
            taskRepository.syncPendingTasks()
            Log.d("SyncWorker", "Sync completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
