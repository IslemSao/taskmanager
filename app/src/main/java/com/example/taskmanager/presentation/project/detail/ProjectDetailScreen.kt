package com.example.taskmanager.presentation.project.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.model.ProjectRole
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
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(projectId) {
        if (projectId.isNullOrEmpty()) {
            viewModel.initializeNewProject()
        } else {
            viewModel.loadProject(projectId)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.isProjectSaved) {
        if (state.isProjectSaved) {
            navController.navigateUp()
        }
    }

    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.project.startDate?.time
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    showStartDatePicker = false
                    startDatePickerState.selectedDateMillis?.let { dateMillis ->
                        viewModel.updateStartDate(Date(dateMillis))
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                title = { Text("Select Start Date") },
                headline = { Text("When does the project start?") },
                showModeToggle = true
            )
        }
    }

    if (showDueDatePicker) {
        val dueDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.project.dueDate?.time
        )
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    showDueDatePicker = false
                    dueDatePickerState.selectedDateMillis?.let { dateMillis ->
                        viewModel.updateDueDate(Date(dateMillis))
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = dueDatePickerState,
                title = { Text("Select Due Date") },
                headline = { Text("When is the project due?") },
                showModeToggle = true
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNewProject) "New Project" else "Edit Project"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!state.isLoading && !state.isSaving) {
                        IconButton(onClick = { viewModel.saveProject() }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Save"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title field
                OutlinedTextField(
                    value = state.project.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description field
                OutlinedTextField(
                    value = state.project.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Replace the entire color picker section with this:

// Color selection
                Text(
                    text = "Project Color",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

// Color picker
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val colors = listOf(
                        Color(0xFFF44336) to "Red",
                        Color(0xFFE91E63) to "Pink",
                        Color(0xFF9C27B0) to "Purple",
                        Color(0xFF673AB7) to "Deep Purple",
                        Color(0xFF3F51B5) to "Indigo",
                        Color(0xFF2196F3) to "Blue",
                        Color(0xFF03A9F4) to "Light Blue",
                        Color(0xFF00BCD4) to "Cyan",
                        Color(0xFF009688) to "Teal",
                        Color(0xFF4CAF50) to "Green",
                        Color(0xFF8BC34A) to "Light Green",
                        Color(0xFFCDDC39) to "Lime",
                        Color(0xFFFFEB3B) to "Yellow",
                        Color(0xFFFFC107) to "Amber",
                        Color(0xFFFF9800) to "Orange",
                        Color(0xFFFF5722) to "Deep Orange"
                    )

                    items(colors) { (color, name) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 2.dp,
                                    color = if (state.project.color == color.toArgb()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateColor(color.toArgb())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.project.color == color.toArgb()) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Date fields
                Text(
                    text = "Project Timeline",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Start date
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Start Date"
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = state.project.startDate?.let { dateFormatter.format(it) } ?: "Not set",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Due date
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDueDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = "Due Date"
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Due Date",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = state.project.dueDate?.let { dateFormatter.format(it) } ?: "Not set",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Project Members section - now shown for both new and existing projects
                Text(
                    text = "Project Members",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List current members if not a new project
                if (!state.isNewProject) {
                    state.project.members.forEach { member ->
                        MemberItem(
                            member = member,
                            isOwner = state.project.ownerId == state.currentUser?.id,
                            onRemove = {
                                if (member.userId != state.project.ownerId) {
                                    viewModel.removeMember(member.userId)
                                }
                            }
                        )
                    }
                }

                // Invite section - available for both new and existing projects
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.inviteEmail,
                    onValueChange = { viewModel.updateInviteEmail(it) },
                    label = { Text("Invite by Email") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (state.isNewProject) {
                                    viewModel.addPendingInvite()
                                } else {

                                    viewModel.inviteMember()
                                }
                            },
                            enabled = state.inviteEmail.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Invite"
                            )
                        }
                    }
                )

                if (state.isInviting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                state.inviteError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Show pending invites for new projects
                if (state.isNewProject && state.pendingInvites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pending Invites",
                        style = MaterialTheme.typography.titleSmall
                    )

                    state.pendingInvites.forEach { email ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removePendingInvite(email) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = { viewModel.saveProject() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && state.project.title.isNotBlank()
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save Project")
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    member: ProjectMember,
    isOwner: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Member info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = member.role.name,
                style = MaterialTheme.typography.bodySmall,
                color = when (member.role) {
                    ProjectRole.OWNER -> MaterialTheme.colorScheme.primary
                    ProjectRole.ADMIN -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Remove button (only for non-owners)
        if (isOwner && member.role != ProjectRole.OWNER) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
