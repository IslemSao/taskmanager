package com.saokt.taskmanager.presentation.project.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.InfoChip
import com.saokt.taskmanager.presentation.components.ProjectMembersSection
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    navController: NavController,
    viewModel: ProjectDetailViewModel,
    projectId: String?
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(projectId) {
        if (projectId.isNullOrEmpty()) viewModel.initializeNewProject() else viewModel.loadProject(projectId)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.isProjectSaved) {
        if (state.isProjectSaved) navController.navigateUp()
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.project.startDate?.time)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    showStartDatePicker = false
                    pickerState.selectedDateMillis?.let { viewModel.updateStartDate(Date(it)) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState, title = { Text("Select start date") })
        }
    }

    if (showDueDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.project.dueDate?.time)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    showDueDatePicker = false
                    pickerState.selectedDateMillis?.let { viewModel.updateDueDate(Date(it)) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState, title = { Text("Select due date") })
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.isNewProject) "New project" else "Edit project",
                subtitle = "Shape the workspace, timeline, and team in one place",
                onBack = { navController.navigateUp() },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveProject() },
                        enabled = !state.isSaving && state.project.title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Save project")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = AppTheme.screenPadding,
                    end = AppTheme.screenPadding,
                    top = 8.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.sectionSpacing)
            ) {
                item {
                    HeroCard(
                        eyebrow = "Project editor",
                        title = if (state.project.title.isBlank()) "Untitled project" else state.project.title,
                        body = "Use projects to give tasks a home, add people, and create a clearer structure for the team.",
                        stats = listOf(
                            "Members" to state.project.members.size.toString(),
                            "Invites" to state.pendingInvites.size.toString()
                        )
                    )
                }

                item {
                    SectionCard(title = "Basics") {
                        OutlinedTextField(
                            value = state.project.title,
                            onValueChange = viewModel::updateTitle,
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.project.description,
                            onValueChange = viewModel::updateDescription,
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }

                item {
                    SectionCard(title = "Color") {
                        val colors = listOf(
                            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF673AB7), Color(0xFF3F51B5),
                            Color(0xFF2196F3), Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
                            Color(0xFF8BC34A), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(colors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = 3.dp,
                                            color = if (state.project.color == color.toArgb()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.updateColor(color.toArgb()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.project.color == color.toArgb()) {
                                        Icon(Icons.Default.Done, contentDescription = "Selected", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = "Timeline") {
                        DateCard(
                            icon = Icons.Default.CalendarToday,
                            label = "Start date",
                            value = state.project.startDate?.let(dateFormatter::format) ?: "Not set",
                            onClick = { showStartDatePicker = true }
                        )
                        DateCard(
                            icon = Icons.Default.EventBusy,
                            label = "Due date",
                            value = state.project.dueDate?.let(dateFormatter::format) ?: "Not set",
                            onClick = { showDueDatePicker = true }
                        )
                    }
                }

                item {
                    SectionCard(title = "Members") {
                        ProjectMembersSection(
                            members = state.project.members,
                            isOwner = state.project.ownerId == state.currentUser?.id,
                            onRemoveMember = { userId ->
                                if (userId != state.project.ownerId) viewModel.removeMember(userId)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!state.isNewProject && state.project.members.isNotEmpty()) {
                            Button(
                                onClick = { navController.navigate(Screen.ProjectMembers.createRoute(state.project.id)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Group, contentDescription = "View members")
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                                Text("View all members")
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = "Invite people") {
                        OutlinedTextField(
                            value = state.inviteEmail,
                            onValueChange = viewModel::updateInviteEmail,
                            label = { Text("Invite by email") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (state.isNewProject) viewModel.addPendingInvite() else viewModel.inviteMember()
                                    },
                                    enabled = state.inviteEmail.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Invite")
                                }
                            }
                        )

                        if (state.isInviting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        state.inviteError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        if (state.isNewProject && state.pendingInvites.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.pendingInvites.forEach { email ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))
                                            Text(email, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        IconButton(onClick = { viewModel.removePendingInvite(email) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove invite")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.saveProject() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !state.isSaving && state.project.title.isNotBlank()
                    ) {
                        Text(if (state.isSaving) "Saving..." else "Save project")
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(14.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
