package com.saokt.taskmanager.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saokt.taskmanager.domain.model.NotificationDestination
import com.saokt.taskmanager.domain.model.NotificationRecord
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.EmptyStateCard
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.InfoChip
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notifications",
                subtitle = "Reminders, updates, and recent activity",
                onBack = { navController.navigateUp() },
                actions = {
                    if (state.notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.NotificationSettings.route) }) {
                        Icon(Icons.Default.Tune, contentDescription = "Notification settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(AppTheme.screenPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "No notifications yet",
                        body = "Task reminders, chat alerts, and weekly summaries will show up here as activity starts.",
                        icon = Icons.Default.Notifications
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = AppTheme.screenPadding,
                        end = AppTheme.screenPadding,
                        top = 8.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.sectionSpacing)
                ) {
                    item {
                        HeroCard(
                            eyebrow = "Inbox",
                            title = "${state.notifications.count { !it.isRead }} unread updates",
                            body = "Stay on top of task reminders, project activity, and chat context without losing your place.",
                            stats = listOf(
                                "Total" to state.notifications.size.toString(),
                                "Unread" to state.notifications.count { !it.isRead }.toString()
                            )
                        )
                    }

                    item {
                        SectionCard(
                            title = "Recent activity",
                            actionLabel = "Clear all",
                            onActionClick = { viewModel.clearAll() }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.notifications.forEach { notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onOpen = {
                                            viewModel.markAsRead(notification.id)
                                            openNotificationTarget(navController, notification)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationRecord,
    onOpen: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .background(
                        color = if (notification.isRead) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        InfoChip(label = "New")
                    }
                }
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeFormatter.format(notification.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun openNotificationTarget(
    navController: NavController,
    notification: NotificationRecord
) {
    when (notification.target.destination) {
        NotificationDestination.TASK_DETAIL -> {
            notification.target.primaryId?.let { navController.navigate(Screen.TaskDetail.createRoute(it)) }
        }
        NotificationDestination.TASK_LIST -> navController.navigate(Screen.TaskList.route)
        NotificationDestination.PROJECT_DETAIL -> {
            notification.target.primaryId?.let { navController.navigate(Screen.ProjectDetail.createRoute(it)) }
        }
        NotificationDestination.PROJECT_LIST -> navController.navigate(Screen.ProjectList.route)
        NotificationDestination.CHAT -> {
            when {
                notification.target.secondaryId != null -> {
                    navController.navigate(
                        Screen.TaskDetail.createRoute(
                            taskId = notification.target.secondaryId,
                            projectId = notification.target.primaryId
                        )
                    )
                }
                notification.target.primaryId != null -> {
                    navController.navigate(Screen.ProjectTasks.createRoute(notification.target.primaryId))
                }
                else -> navController.navigate(Screen.Notifications.route)
            }
        }
        NotificationDestination.NOTIFICATIONS -> Unit
    }
}
