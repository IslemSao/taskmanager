package com.saokt.taskmanager.presentation.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.saokt.taskmanager.domain.model.*
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
            TopAppBar(
                title = { 
                    Text(
                        text = "Enhanced Notification Settings",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Notifications Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                            text = "🔔 Master Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                            text = "Enable or disable all notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                        checked = state.globalSettings.masterNotificationsEnabled,
                        onCheckedChange = { viewModel.toggleMasterNotifications(it) }
                    )
                }
            }
        }

        // Quiet Hours Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                    text = "🌙 Quiet Hours",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                        text = "Enable Quiet Hours",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                        checked = state.globalSettings.quietHoursEnabled,
                        onCheckedChange = { viewModel.toggleQuietHours(it) }
                        )
                    }

                if (state.globalSettings.quietHoursEnabled) {
                    Divider()

                    var showStartTimePicker by remember { mutableStateOf(false) }
                    var showEndTimePicker by remember { mutableStateOf(false) }

                                        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

                    // Start Time Picker Dialog
                    if (showStartTimePicker) {
                        SimpleTimePickerDialog(
                            title = "Select Start Time",
                            currentTime = state.globalSettings.quietHoursStart,
                            onDismissRequest = { showStartTimePicker = false },
                            onTimeSelected = { newTime ->
                                viewModel.updateQuietHoursStart(newTime)
                                showStartTimePicker = false
                            }
                        )
                    }

                    // End Time Picker Dialog
                    if (showEndTimePicker) {
                        SimpleTimePickerDialog(
                            title = "Select End Time",
                            currentTime = state.globalSettings.quietHoursEnd,
                            onDismissRequest = { showEndTimePicker = false },
                            onTimeSelected = { newTime ->
                                viewModel.updateQuietHoursEnd(newTime)
                                showEndTimePicker = false
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.globalSettings.quietHoursStart.format(timeFormatter),
                            onValueChange = { },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showStartTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Select start time")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = state.globalSettings.quietHoursEnd.format(timeFormatter),
                            onValueChange = { },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showEndTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Select end time")
                                }
                            }
                        )
                    }

                    Text(
                        text = "Notifications will be silenced during these hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Notification Types Section
        Text(
            text = "📋 Notification Types",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        state.notificationPreferences.forEach { preference ->
            NotificationTypeCard(
                preference = preference,
                onToggle = { viewModel.toggleNotificationType(preference.type, it) },
                onTest = { viewModel.testNotification(preference.type) },
                onUpdatePriority = { viewModel.updateNotificationPriority(preference.type, it) }
            )
            }

            // Action Buttons
            Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 16.dp)
            ) {
                            Button(
                onClick = { viewModel.testNotification(NotificationType.TASK_REMINDER) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔔 Test Default Notification")
            }

            OutlinedButton(
                onClick = { viewModel.testPersistence() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Test Persistence")
            }

            OutlinedButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 Reset to Defaults")
            }
            }

            // Status Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                    text = "📊 Status",
                        style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Master notifications: ${if (state.globalSettings.masterNotificationsEnabled) "✅ Enabled" else "❌ Disabled"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Quiet hours: ${if (state.globalSettings.quietHoursEnabled) "🌙 Active" else "☀️ Inactive"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Active notification types: ${state.notificationPreferences.count { it.enabled }}/${state.notificationPreferences.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTypeCard(
    preference: NotificationPreference,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onUpdatePriority: (NotificationPriority) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    Text(
                        text = getNotificationTypeDisplayName(preference.type),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = getNotificationTypeDescription(preference.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = preference.enabled,
                    onCheckedChange = onToggle
                )
            }

            if (preference.enabled) {
                Divider()

                // Priority Selection
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.bodyMedium
                )

                var priorityExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = preference.priority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        NotificationPriority.values().forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.name) },
                                onClick = {
                                    onUpdatePriority(priority)
                                    priorityExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // Schedule Info
                if (preference.scheduleConfig.intervalHours > 0) {
                    Text(
                        text = "Interval: Every ${preference.scheduleConfig.intervalHours} hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Test Button
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔔 Test This Notification")
                }
            }
        }
    }
}

private fun getNotificationTypeDisplayName(type: NotificationType): String {
    return when (type) {
        NotificationType.TASK_REMINDER -> "📝 Task Reminders"
        NotificationType.DUE_SOON_ALERT -> "⏰ Due Soon Alerts"
        NotificationType.OVERDUE_ALERT -> "⚠️ Overdue Alerts"
        NotificationType.HIGH_PRIORITY_REMINDER -> "🔥 High Priority Reminders"
        NotificationType.PROJECT_UPDATE -> "📁 Project Updates"
        NotificationType.ASSIGNMENT_UPDATE -> "👤 Assignment Updates"
        NotificationType.COMPLETION_CELEBRATION -> "🎉 Completion Celebrations"
        NotificationType.STREAK_REMINDER -> "🔥 Streak Reminders"
        NotificationType.DEADLINE_WARNING -> "⚡ Deadline Warnings"
        NotificationType.WEEKLY_SUMMARY -> "📊 Weekly Summaries"
        NotificationType.MORNING_BRIEFING -> "🌅 Morning Briefings"
        NotificationType.EVENING_REVIEW -> "🌙 Evening Reviews"
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
    // Common time options for quiet hours - focused on evening/bedtime to morning/wake up
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
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Current selection preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Time options grid
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(timeOptions) { (displayText, time) ->
                        OutlinedButton(
                            onClick = { selectedTime = time },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedTime == time)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selectedTime == time)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedTime == time)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onTimeSelected(selectedTime)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Time")
                    }
                }
            }
        }
    }
}
