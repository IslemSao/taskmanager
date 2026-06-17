package com.saokt.taskmanager.presentation.authentication.signin

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.saokt.taskmanager.R
import com.saokt.taskmanager.data.util.FirebaseConfig
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.InfoChip
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.AppTheme
import kotlinx.coroutines.launch

private const val TAG = "SignInScreen"

object SignInTestTags {
    const val ROOT = "sign_in_root"
    const val TITLE = "sign_in_title"
    const val SUBTITLE = "sign_in_subtitle"
    const val EMAIL_FIELD = "sign_in_email"
    const val PASSWORD_FIELD = "sign_in_password"
    const val PASSWORD_TOGGLE = "sign_in_password_toggle"
    const val SUBMIT_BUTTON = "sign_in_submit"
    const val FORGOT_PASSWORD_BUTTON = "sign_in_forgot_password"
    const val GOOGLE_BUTTON = "sign_in_google"
    const val SIGN_UP_BUTTON = "sign_in_sign_up"
}

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In Activity Result: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken.isNullOrBlank()) {
                    Log.w(TAG, "Google sign-in returned no ID token.")
                    viewModel.setError("Google sign-in did not return an ID token.")
                } else {
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign-in failed", e)
                viewModel.setError("Google sign-in failed: ${e.statusCode}")
            }
        } else {
            Log.w(TAG, "Google sign-in was cancelled or failed before returning an account.")
            viewModel.setError("Google sign-in was cancelled.")
        }
    }

    SignInScreenContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSignInClick = viewModel::signIn,
        onForgotPasswordClick = viewModel::sendPasswordResetEmail,
        onGoogleSignInClick = {
            Log.d(TAG, "Google sign-in button clicked")
            val webClientId = FirebaseConfig.getGoogleWebClientId(context)
            if (webClientId.isNullOrBlank()) {
                Log.w(TAG, "Google sign-in cannot start because default_web_client_id is missing.")
                viewModel.setError("Google sign-in is not configured for this build.")
            } else {
                try {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    Log.d(TAG, "Launching Google sign-in intent")
                    launcher.launch(googleSignInClient.signInIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch Google sign-in", e)
                    viewModel.setError("Unable to start Google sign-in.")
                }
            }
        },
        onSignUpClick = { navController.navigate(Screen.SignUp.route) },
        onErrorShown = viewModel::clearError,
        onMessageShown = viewModel::clearMessage,
        onSignedIn = {
            navController.navigate(Screen.MainTabs.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
        }
    )
}

@Composable
fun SignInScreenContent(
    state: SignInState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onErrorShown: () -> Unit,
    onMessageShown: () -> Unit,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch { snackbarHostState.showSnackbar(it) }
            onErrorShown()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch { snackbarHostState.showSnackbar(it) }
            onMessageShown()
        }
    }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(SignInTestTags.ROOT),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppTheme.screenPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                HeroCard(
                    eyebrow = "Taskmanager",
                    title = "A better home for your tasks and projects",
                    body = "Sign in to pick up your day with cleaner navigation, stronger focus, and a calmer workspace.",
                    stats = listOf("Tasks" to "Focused", "Projects" to "Clear", "Updates" to "Instant")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = AppTheme.cardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Welcome back",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.testTag(SignInTestTags.TITLE)
                            )
                            Text(
                                text = "Use email or Google to access your workspace.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(SignInTestTags.SUBTITLE)
                            )
                        }

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChanged,
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(SignInTestTags.EMAIL_FIELD)
                        )

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChanged,
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.testTag(SignInTestTags.PASSWORD_TOGGLE)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(SignInTestTags.PASSWORD_FIELD)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoChip(label = "Secure sign-in")
                            TextButton(
                                onClick = onForgotPasswordClick,
                                enabled = !state.isLoading,
                                modifier = Modifier.testTag(SignInTestTags.FORGOT_PASSWORD_BUTTON)
                            ) {
                                Text("Forgot password?")
                            }
                        }

                        Button(
                            onClick = onSignInClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag(SignInTestTags.SUBMIT_BUTTON),
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Sign In")
                            }
                        }

                        OutlinedButton(
                            onClick = onGoogleSignInClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag(SignInTestTags.GOOGLE_BUTTON),
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(22.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Text("Continue with Google")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New here?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onSignUpClick,
                                enabled = !state.isLoading,
                                modifier = Modifier.testTag(SignInTestTags.SIGN_UP_BUTTON)
                            ) {
                                Text("Create an account")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your tasks, projects, reminders, and team context stay in one place.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
