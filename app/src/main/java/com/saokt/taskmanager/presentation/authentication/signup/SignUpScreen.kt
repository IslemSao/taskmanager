package com.saokt.taskmanager.presentation.authentication.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.InfoChip
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.AppTheme
import kotlinx.coroutines.launch

object SignUpTestTags {
    const val ROOT = "sign_up_root"
    const val TITLE = "sign_up_title"
    const val SUBTITLE = "sign_up_subtitle"
    const val DISPLAY_NAME_FIELD = "sign_up_display_name"
    const val EMAIL_FIELD = "sign_up_email"
    const val PASSWORD_FIELD = "sign_up_password"
    const val PASSWORD_TOGGLE = "sign_up_password_toggle"
    const val CONFIRM_PASSWORD_FIELD = "sign_up_confirm_password"
    const val CONFIRM_PASSWORD_TOGGLE = "sign_up_confirm_password_toggle"
    const val SUBMIT_BUTTON = "sign_up_submit"
    const val SIGN_IN_BUTTON = "sign_up_sign_in"
}

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel
) {
    val state by viewModel.state.collectAsState()

    SignUpScreenContent(
        state = state,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onSignUpClick = viewModel::signUp,
        onSignInClick = navController::navigateUp,
        onErrorShown = viewModel::clearError,
        onSignedUp = {
            navController.navigate(Screen.MainTabs.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
        }
    )
}

@Composable
fun SignUpScreenContent(
    state: SignUpState,
    onDisplayNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit,
    onErrorShown: () -> Unit,
    onSignedUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch { snackbarHostState.showSnackbar(it) }
            onErrorShown()
        }
    }

    LaunchedEffect(state.isSignedUp) {
        if (state.isSignedUp) onSignedUp()
    }

    Scaffold(
        topBar = {
            SignUpTopBar(
                title = "Create account",
                subtitle = "Set up your workspace once and keep momentum after that",
                onBack = onSignInClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag(SignUpTestTags.ROOT)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppTheme.screenPadding)
                    .padding(top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                HeroCard(
                    eyebrow = "Get started",
                    title = "Build a cleaner workspace from day one",
                    body = "Create an account to organize projects, assign work, and keep timelines easier to follow.",
                    stats = listOf("Setup" to "2 min", "Projects" to "Shared", "Tracking" to "Simple")
                )

                Card(
                    shape = AppTheme.cardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Join Task Manager",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.testTag(SignUpTestTags.TITLE)
                            )
                            Text(
                                text = "Create your account and start planning work with more clarity.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(SignUpTestTags.SUBTITLE)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            OutlinedTextField(
                                value = state.displayName,
                                onValueChange = onDisplayNameChanged,
                                label = { Text("Full name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SignUpTestTags.DISPLAY_NAME_FIELD)
                            )

                            OutlinedTextField(
                                value = state.email,
                                onValueChange = onEmailChanged,
                                label = { Text("Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SignUpTestTags.EMAIL_FIELD)
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
                                        modifier = Modifier.testTag(SignUpTestTags.PASSWORD_TOGGLE)
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SignUpTestTags.PASSWORD_FIELD)
                            )

                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = onConfirmPasswordChanged,
                                label = { Text("Confirm password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                        modifier = Modifier.testTag(SignUpTestTags.CONFIRM_PASSWORD_TOGGLE)
                                    ) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SignUpTestTags.CONFIRM_PASSWORD_FIELD)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoChip(label = "Free to start")
                            InfoChip(label = "Project-ready")
                        }

                        Button(
                            onClick = onSignUpClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag(SignUpTestTags.SUBMIT_BUTTON),
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
                                Text("Create Account")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onSignInClick,
                                enabled = !state.isLoading,
                                modifier = Modifier.testTag(SignUpTestTags.SIGN_IN_BUTTON)
                            ) {
                                Text("Sign in")
                            }
                        }
                    }
                }

                Text(
                    text = "Teams, deadlines, and personal tasks become much easier to manage once everything lives in one place.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Sign-up header without [TopAppBar] min-height / extra top padding. */
@Composable
private fun SignUpTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
