package com.saokt.taskmanager.presentation.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.EmptyStateCard
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.InfoChip
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.AppTheme

object ProfileTestTags {
    const val ROOT = "profile_root"
    const val LOADING = "profile_loading"
    const val DELETE_LOADING = "profile_delete_loading"
    const val USER_INFO_CARD = "profile_user_info"
    const val VERIFICATION_STATUS = "profile_verification_status"
    const val SEND_VERIFICATION_BUTTON = "profile_send_verification"
    const val REFRESH_VERIFICATION_BUTTON = "profile_refresh_verification"
    const val DELETE_ACCOUNT_BUTTON = "profile_delete_account"
}

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSendVerificationEmailClick = viewModel::sendEmailVerification,
        onRefreshVerificationClick = { viewModel.refreshEmailVerificationStatus(forceRefresh = true) },
        onDeleteAccountClick = viewModel::showDeleteConfirmation,
        onBack = { navController.navigateUp() }
    )

    if (uiState.showDeleteConfirmation) {
        DeleteAccountConfirmationDialog(
            onConfirm = { viewModel.deleteAccount() },
            onDismiss = { viewModel.hideDeleteConfirmation() }
        )
    }
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onSendVerificationEmailClick: () -> Unit,
    onRefreshVerificationClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Profile",
                subtitle = "Your account, verification, and security settings",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(modifier)
                .testTag(ProfileTestTags.ROOT)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(ProfileTestTags.LOADING)
                    )
                }

                uiState.isDeletingAccount -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag(ProfileTestTags.DELETE_LOADING))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Deleting account...", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                uiState.user != null -> {
                    ProfileContent(
                        user = uiState.user!!,
                        isEmailVerified = uiState.isEmailVerified,
                        isSendingVerificationEmail = uiState.isSendingVerificationEmail,
                        onSendVerificationEmailClick = onSendVerificationEmailClick,
                        onRefreshVerificationClick = onRefreshVerificationClick,
                        onDeleteAccountClick = onDeleteAccountClick
                    )
                }

                else -> {
                    EmptyStateCard(
                        title = "Unable to load profile",
                        body = "Try again in a moment. If the issue continues, sign out and back in."
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: com.saokt.taskmanager.domain.model.User,
    isEmailVerified: Boolean?,
    isSendingVerificationEmail: Boolean,
    onSendVerificationEmailClick: () -> Unit,
    onRefreshVerificationClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = AppTheme.screenPadding,
            end = AppTheme.screenPadding,
            top = 8.dp,
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.sectionSpacing)
    ) {
        item {
            HeroCard(
                eyebrow = "Account",
                title = user.displayName ?: user.email.substringBefore("@"),
                body = "Keep your profile up to date and make sure your email stays verified for a smoother experience.",
                stats = listOf(
                    "Email" to "Connected",
                    "Status" to when (isEmailVerified) {
                        true -> "Verified"
                        false -> "Pending"
                        null -> "Checking"
                    }
                )
            )
        }

        item {
            SectionCard(title = "User information", modifier = Modifier.testTag(ProfileTestTags.USER_INFO_CARD)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.size(84.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (user.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Default profile",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = user.displayName ?: "No display name", style = MaterialTheme.typography.headlineSmall)
                        Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        InfoChip(
                            label = when (isEmailVerified) {
                                true -> "Email verified"
                                false -> "Verification needed"
                                null -> "Checking status"
                            },
                            modifier = Modifier.testTag(ProfileTestTags.VERIFICATION_STATUS)
                        )
                    }
                }
            }
        }

        if (isEmailVerified != true) {
            item {
                SectionCard(title = "Verify your email") {
                    Text(
                        text = "Verification unlocks a smoother recovery flow and helps keep your account secure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onSendVerificationEmailClick,
                        enabled = !isSendingVerificationEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ProfileTestTags.SEND_VERIFICATION_BUTTON)
                    ) {
                        if (isSendingVerificationEmail) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            androidx.compose.material3.Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Send verification email")
                    }
                    OutlinedButton(
                        onClick = onRefreshVerificationClick,
                        enabled = !isSendingVerificationEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ProfileTestTags.REFRESH_VERIFICATION_BUTTON)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Refresh verification status")
                    }
                }
            }
        }

        item {
            SectionCard(title = "Danger zone") {
                Text(
                    text = "Deleting your account permanently removes your access and associated personal data for this workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onDeleteAccountClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ProfileTestTags.DELETE_ACCOUNT_BUTTON),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Delete account")
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = {
            Text("This action is permanent and cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
