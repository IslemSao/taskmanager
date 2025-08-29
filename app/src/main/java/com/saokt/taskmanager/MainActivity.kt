package com.saokt.taskmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.saokt.taskmanager.notification.FCMTokenManager
import com.saokt.taskmanager.presentation.authentication.AuthViewModel
import com.saokt.taskmanager.presentation.navigation.AppNavGraph
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.TaskmanagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    // Flag to track if authentication check is complete
    private var isAuthReady = false

    // Initial route based on auth status
    private var startDestination = Screen.SignIn.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        // Check authentication state before continuing
        checkAuthStateAndSetRoute()
    }

    override fun onResume() {
        super.onResume()
        // Refresh FCM token every time the app comes to foreground
        refreshFCMToken()
    }
    
    private fun checkAuthStateAndSetRoute() {
        // Check auth state using ViewModel with UseCase
        val isAuthenticated = authViewModel.isUserAuthenticated()

        Log.d("MainActivity", "Auth check using ViewModel and UseCase - User is authenticated: $isAuthenticated")

        // Set the start destination based on authentication state
        startDestination = if (isAuthenticated) {
            Screen.Dashboard.route
        } else {
            Screen.SignIn.route
        }

        // Mark auth check as complete and proceed with UI initialization
        isAuthReady = true

        // Refresh FCM token if user is authenticated
        if (isAuthenticated) {
            refreshFCMToken()
        }

        initializeUI()
    }
    
    private fun initializeUI() {
        setContent {
            TaskmanagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the start destination directly
                    AppNavGraph(startDestination = startDestination)
                }
            }
        }
    }

    /**
     * Refresh FCM token and update it in Firestore
     */
    private fun refreshFCMToken() {
        lifecycleScope.launch {
            try {
                val result = fcmTokenManager.refreshAndUpdateToken()
                result.onSuccess { token ->
                    Log.d("MainActivity", "FCM token refreshed successfully: ${token.take(20)}...")
                }.onFailure { exception ->
                    Log.e("MainActivity", "Failed to refresh FCM token", exception)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error refreshing FCM token", e)
            }
        }
    }
}
