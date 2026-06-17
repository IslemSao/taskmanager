package com.saokt.taskmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.saokt.taskmanager.data.util.FirebaseConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.saokt.taskmanager.notification.FCMTokenManager
import com.saokt.taskmanager.presentation.authentication.AuthViewModel
import com.saokt.taskmanager.presentation.navigation.AppNavGraph
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.TaskmanagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    sealed interface UpdatePromptState {
        data object None : UpdatePromptState
        data class Optional(val latestVersionCode: Int) : UpdatePromptState
        data class Required(val minSupportedVersionCode: Int) : UpdatePromptState
    }

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    // Flag to track if authentication check is complete
    private var isAuthReady = false

    // Initial route based on auth status
    private var startDestination = Screen.SignIn.route
    private var updatePromptState: UpdatePromptState by mutableStateOf(UpdatePromptState.None)

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
        checkForAppUpdate()
    }
    
    private fun checkAuthStateAndSetRoute() {
        // Check auth state using ViewModel with UseCase
        val isAuthenticated = authViewModel.isUserAuthenticated()

        Log.d("MainActivity", "Auth check using ViewModel and UseCase - User is authenticated: $isAuthenticated")

        // Set the start destination based on authentication state
        startDestination = if (isAuthenticated) {
            Screen.MainTabs.route
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
        checkForAppUpdate()
    }
    
    private fun initializeUI() {
        setContent {
            TaskmanagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(startDestination = startDestination)
                    UpdatePromptDialog(
                        state = updatePromptState,
                        onDismissOptional = { updatePromptState = UpdatePromptState.None },
                        onUpdateNow = { openPlayStoreListing() }
                    )
                }
            }
        }
    }

    private fun checkForAppUpdate() {
        if (!FirebaseConfig.isConfigured(this)) {
            Log.w("MainActivity", "Skipping update check because Firebase is not configured.")
            return
        }

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 60 * 60
            }
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                "min_supported_version_code" to BuildConfig.VERSION_CODE.toLong(),
                "latest_available_version_code" to BuildConfig.VERSION_CODE.toLong(),
            )
        )

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Update check fetch failed", task.exception)
                return@addOnCompleteListener
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            val minSupportedVersionCode = remoteConfig.getLong("min_supported_version_code").toInt()
            val latestAvailableVersionCode = remoteConfig.getLong("latest_available_version_code").toInt()

            updatePromptState = when {
                currentVersionCode < minSupportedVersionCode ->
                    UpdatePromptState.Required(minSupportedVersionCode)
                currentVersionCode < latestAvailableVersionCode ->
                    UpdatePromptState.Optional(latestAvailableVersionCode)
                else -> UpdatePromptState.None
            }
        }
    }

    private fun openPlayStoreListing() {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(marketIntent)
        } catch (_: Exception) {
            startActivity(webIntent)
        }
    }

    /**
     * Refresh FCM token and update it in Firestore
     */
    private fun refreshFCMToken() {
        if (!FirebaseConfig.isConfigured(this)) {
            Log.w("MainActivity", "Skipping FCM token refresh because Firebase is not configured.")
            return
        }

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

@androidx.compose.runtime.Composable
private fun UpdatePromptDialog(
    state: MainActivity.UpdatePromptState,
    onDismissOptional: () -> Unit,
    onUpdateNow: () -> Unit,
) {
    val isRequired = state is MainActivity.UpdatePromptState.Required
    if (state is MainActivity.UpdatePromptState.None) return

    if (isRequired) {
        BackHandler(enabled = true) {
            // Required update: ignore back press while dialog is displayed.
        }
    }

    val message = when (state) {
        is MainActivity.UpdatePromptState.Required ->
            "A new version is required to keep using the app. Please update to continue."
        is MainActivity.UpdatePromptState.Optional ->
            "A new version is available (${state.latestVersionCode}). Update now for the latest improvements."
        MainActivity.UpdatePromptState.None -> ""
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRequired) onDismissOptional()
        },
        title = {
            Text(if (isRequired) "Update required" else "Update available")
        },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onUpdateNow) {
                Text("Update now")
            }
        },
        dismissButton = if (isRequired) {
            null
        } else {
            {
                Button(onClick = onDismissOptional) {
                    Text("Later")
                }
            }
        }
    )
}
