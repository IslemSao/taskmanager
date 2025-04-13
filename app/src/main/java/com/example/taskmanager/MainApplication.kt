package com.example.taskmanager

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.taskmanager.data.util.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject lateinit var syncManager: SyncManager
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic sync
        syncManager.schedulePeriodicSync()

        // Set up network listener to trigger sync when connectivity is restored
        setupNetworkCallback()
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
                println("Network available - triggering sync")
                syncManager.scheduleOneTimeSync()
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
}