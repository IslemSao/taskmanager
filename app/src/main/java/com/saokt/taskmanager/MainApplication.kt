package com.saokt.taskmanager

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.saokt.taskmanager.data.util.FirebaseConfig
import com.saokt.taskmanager.data.util.SyncManager
import com.saokt.taskmanager.notification.TaskNotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {
    @Inject lateinit var syncManagerProvider: Provider<SyncManager>
    @Inject lateinit var notificationScheduler: TaskNotificationScheduler
    @Inject lateinit var workerFactory: HiltWorkerFactory
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Start notification scheduling
        notificationScheduler.startNotificationScheduling()

        if (FirebaseConfig.isConfigured(this)) {
            syncManagerProvider.get().schedulePeriodicSync()
            setupNetworkCallback()
        } else {
            Log.w("MainApplication", "Firebase config is missing. Running without remote sync.")
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    @SuppressLint("ServiceCast")
    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // This is called when network becomes available
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Network is available, trigger sync
                Log.d("MainApplication", "Network available - triggering sync")
                if (FirebaseConfig.isConfigured(this@MainApplication)) {
                    syncManagerProvider.get().scheduleOneTimeSync()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
