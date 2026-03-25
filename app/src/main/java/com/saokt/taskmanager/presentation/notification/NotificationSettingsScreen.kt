package com.saokt.taskmanager.presentation.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.saokt.taskmanager.domain.model.NotificationPreference
import com.saokt.taskmanager.domain.model.NotificationPriority
import com.saokt.taskmanager.domain.model.NotificationType
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.ui.theme.AppTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notification settings",
                subtitle = "Tune what reaches you and when",
                onBack = { navController.navigateUp() },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { viewModel.resetToDefaults() }) {
                        androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            EnhancedNotificationSettingsContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedNotificationSettingsContent(
    state: EnhancedNotificationSettingsState,
    viewModel: NotificationSettingsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                eyebrow = "Controls",
                title = "${state.notificationPreferences.count { it.enabled }} notification types active",
                body = "Keep important alerts on, quiet the noise, and use quiet hours to protect deep work or sleep.",
                stats = listOf(
                    "Master" to if (state.globalSettings.masterNotificationsEnabled) "On" else "Off",
                    "Quiet hours" to if (state.globalSettings.quietHoursEnabled) "On" else "Off"
                )
            )
        }

        item {
            SectionCard(title = "Global controls") {
                SettingSwitchRow(
                    title = "Master notifications",
                    subtitle = "Enable or disable all notifications at once",
                    checked = state.globalSettings.masterNotificationsEnabled,
                    onCheckedChange = viewModel::toggleMasterNotifications
                )
                HorizontalDivider()
                SettingSwitchRow(
                    title = "Quiet hours",
                    subtitle = "Silence notifications during protected hours",
                    checked = state.globalSettings.quietHoursEnabled,
                    onCheckedChange = viewModel::toggleQuietHours
                )
                if (state.globalSettings.quietHoursEnabled) {
                    QuietHoursControls(
                        start = state.globalSettings.quietHoursStart,
                        end = state.globalSettings.quietHoursEnd,
                        onUpdateStart = viewModel::updateQuietHoursStart,
                        onUpdateEnd = viewModel::updateQuietHoursEnd
                    )
                }
            }
        }

        item {
            SectionCard(title = "Notification types") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.notificationPreferences.forEach { preference ->
                        NotificationTypeCard(
                            preference = preference,
                            onToggle = { viewModel.toggleNotificationType(preference.type, it) },
                            onUpdatePriority = { viewModel.updateNotificationPriority(preference.type, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun QuietHoursControls(
    start: LocalTime,
    end: LocalTime,
    onUpdateStart: (LocalTime) -> Unit,
    onUpdateEnd: (LocalTime) -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    if (showStartTimePicker) {
        SimpleTimePickerDialog(
            title = "Select quiet hours start",
            currentTime = start,
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = {
                onUpdateStart(it)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        SimpleTimePickerDialog(
            title = "Select quiet hours end",
            currentTime = end,
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = {
                onUpdateEnd(it)
                showEndTimePicker = false
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = start.format(timeFormatter),
            onValueChange = {},
            label = { Text("Start") },
            readOnly = true,
            trailingIcon = {
                androidx.compose.material3.IconButton(onClick = { showStartTimePicker = true }) {
                    androidx.compose.material3.Icon(Icons.Default.Schedule, contentDescription = "Select start time")
                }
            },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = end.format(timeFormatter),
            onValueChange = {},
            label = { Text("End") },
            readOnly = true,
            trailingIcon = {
                androidx.compose.material3.IconButton(onClick = { showEndTimePicker = true }) {
                    androidx.compose.material3.Icon(Icons.Default.Schedule, contentDescription = "Select end time")
                }
            },
            modifier = Modifier.weight(1f)
        )
    }

    Text(
        text = "Notifications will stay quiet between these times.",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTypeCard(
    preference: NotificationPreference,
    onToggle: (Boolean) -> Unit,
    onUpdatePriority: (NotificationPriority) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = getNotificationTypeDisplayName(preference.type), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    Text(
                        text = getNotificationTypeDescription(preference.type),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = preference.enabled, onCheckedChange = onToggle)
            }

            if (preference.enabled) {
                HorizontalDivider()
                var priorityExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { priorityExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Priority: ${preference.priority.name.lowercase().replaceFirstChar(Char::titlecase)}")
                    }
                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        NotificationPriority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.name.lowercase().replaceFirstChar(Char::titlecase)) },
                                onClick = {
                                    onUpdatePriority(priority)
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                if (preference.scheduleConfig.intervalHours > 0) {
                    Text(
                        text = "Interval: every ${preference.scheduleConfig.intervalHours} hours",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getNotificationTypeDisplayName(type: NotificationType): String {
    return when (type) {
        NotificationType.TASK_REMINDER -> "Task reminders"
        NotificationType.DUE_SOON_ALERT -> "Due soon alerts"
        NotificationType.OVERDUE_ALERT -> "Overdue alerts"
        NotificationType.HIGH_PRIORITY_REMINDER -> "High priority reminders"
        NotificationType.PROJECT_UPDATE -> "Project updates"
        NotificationType.ASSIGNMENT_UPDATE -> "Assignment updates"
        NotificationType.COMPLETION_CELEBRATION -> "Completion celebrations"
        NotificationType.STREAK_REMINDER -> "Streak reminders"
        NotificationType.DEADLINE_WARNING -> "Deadline warnings"
        NotificationType.WEEKLY_SUMMARY -> "Weekly summaries"
        NotificationType.MORNING_BRIEFING -> "Morning briefings"
        NotificationType.EVENING_REVIEW -> "Evening reviews"
        NotificationType.CHAT_MESSAGE -> "Chat messages"
    }
}

private fun getNotificationTypeDescription(type: NotificationType): String {
    return when (type) {
        NotificationType.TASK_REMINDER -> "Periodic reminders for pending tasks"
        NotificationType.DUE_SOON_ALERT -> "Alerts for tasks due within 24 hours"
        NotificationType.OVERDUE_ALERT -> "Notifications for overdue tasks"
        NotificationType.HIGH_PRIORITY_REMINDER -> "Special reminders for high priority tasks"
        NotificationType.PROJECT_UPDATE -> "Updates when projects are modified"
        NotificationType.ASSIGNMENT_UPDATE -> "Notifications for new task assignments"
        NotificationType.COMPLETION_CELEBRATION -> "Celebratory messages for task completion"
        NotificationType.STREAK_REMINDER -> "Reminders to maintain your productivity streak"
        NotificationType.DEADLINE_WARNING -> "Warnings for approaching deadlines"
        NotificationType.WEEKLY_SUMMARY -> "Weekly overview of your productivity"
        NotificationType.MORNING_BRIEFING -> "Daily morning task overview"
        NotificationType.EVENING_REVIEW -> "Daily evening productivity review"
        NotificationType.CHAT_MESSAGE -> "Notifications for new chat messages"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTimePickerDialog(
    title: String,
    currentTime: LocalTime,
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    val timeOptions = listOf(
        "8:00 PM" to LocalTime.of(20, 0),
        "8:30 PM" to LocalTime.of(20, 30),
        "9:00 PM" to LocalTime.of(21, 0),
        "9:30 PM" to LocalTime.of(21, 30),
        "10:00 PM" to LocalTime.of(22, 0),
        "10:30 PM" to LocalTime.of(22, 30),
        "11:00 PM" to LocalTime.of(23, 0),
        "11:30 PM" to LocalTime.of(23, 30),
        "12:00 AM" to LocalTime.of(0, 0),
        "12:30 AM" to LocalTime.of(0, 30),
        "1:00 AM" to LocalTime.of(1, 0),
        "1:30 AM" to LocalTime.of(1, 30),
        "2:00 AM" to LocalTime.of(2, 0),
        "3:00 AM" to LocalTime.of(3, 0),
        "4:00 AM" to LocalTime.of(4, 0),
        "5:00 AM" to LocalTime.of(5, 0),
        "5:30 AM" to LocalTime.of(5, 30),
        "6:00 AM" to LocalTime.of(6, 0),
        "6:30 AM" to LocalTime.of(6, 30),
        "7:00 AM" to LocalTime.of(7, 0),
        "7:30 AM" to LocalTime.of(7, 30),
        "8:00 AM" to LocalTime.of(8, 0),
        "8:30 AM" to LocalTime.of(8, 30),
        "9:00 AM" to LocalTime.of(9, 0),
        "9:30 AM" to LocalTime.of(9, 30),
        "10:00 AM" to LocalTime.of(10, 0)
    )

    var selectedTime by remember { mutableStateOf(currentTime) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Selected: ${selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(timeOptions) { (displayText, time) ->
                        OutlinedButton(
                            onClick = { selectedTime = time },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedTime == time) {
                                    androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    androidx.compose.material3.MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedTime == time) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(text = displayText)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = { onTimeSelected(selectedTime) }, modifier = Modifier.weight(1f)) {
                        Text("Set time")
                    }
                }
            }
        }
    }
}
