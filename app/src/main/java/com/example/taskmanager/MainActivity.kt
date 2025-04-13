package com.example.taskmanager

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
import com.example.taskmanager.presentation.authentication.signin.SignInViewModel
import com.example.taskmanager.presentation.navigation.AppNavGraph
import com.example.taskmanager.presentation.navigation.Screen
import com.example.taskmanager.ui.theme.TaskmanagerTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    // Flag to track if authentication check is complete
    private var isAuthReady = false
    
    // Initial route based on auth status
    private var startDestination = Screen.SignIn.route

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (must be before super.onCreate)
        val splashScreen = installSplashScreen()
        
        // Keep splash screen visible until auth state is determined
        splashScreen.setKeepOnScreenCondition { !isAuthReady }
        
        super.onCreate(savedInstanceState)
        
        // Check authentication state before continuing
        checkAuthStateAndSetRoute()
    }
    
    private fun checkAuthStateAndSetRoute() {
        // Check Firebase Auth directly instead of using ViewModel
        val currentUser = firebaseAuth.currentUser
        val isAuthenticated = currentUser != null
        
        Log.d("MainActivity", "Direct Firebase Auth check - User is authenticated: $isAuthenticated")
        
        // Set the start destination based on authentication state
        startDestination = if (isAuthenticated) {
            Screen.Dashboard.route
        } else {
            Screen.SignIn.route
        }
        
        // Mark auth check as complete and proceed with UI initialization
        isAuthReady = true
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
}
