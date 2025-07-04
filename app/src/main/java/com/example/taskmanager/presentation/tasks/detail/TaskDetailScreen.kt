package com.example.taskmanager.presentation.tasks.detail

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskmanager.domain.model.Priority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel,
    taskId: String
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.isTaskSaved) {
        if (state.isTaskSaved) {
            navController.navigateUp()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNewTask) "New Task" else "Edit Task",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isLoading && !state.isSaving) {
                        IconButton(
                            onClick = { viewModel.saveTask() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title field
                OutlinedTextField(
                    value = state.task.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Description field
                OutlinedTextField(
                    value = state.task.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Priority selector
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Priority.values().forEach { priority ->
                        SegmentedButton(
                            selected = state.task.priority == priority,
                            onClick = { viewModel.updatePriority(priority) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = priority.ordinal,
                                count = Priority.values().size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = when (priority) {
                                    Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                                    Priority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                                    Priority.LOW -> MaterialTheme.colorScheme.primaryContainer
                                },
                                activeContentColor = when (priority) {
                                    Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                    Priority.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
                                    Priority.LOW -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        ) {
                            Text(priority.name)
                        }
                    }
                }
            Spacer(modifier = Modifier.height(24.dp))

                // Project selector
                Text(
                    text = "Assign to Project",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                var isProjectDropdownExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isProjectDropdownExpanded,
                    onExpandedChange = { isProjectDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = state.availableProjects.find { it.id == state.task.projectId }?.title ?: "No Project",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Project") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProjectDropdownExpanded)
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isProjectDropdownExpanded,
                        onDismissRequest = { isProjectDropdownExpanded = false }
                    ) {
                        // Add "No Project" option
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "No Project",
                                    style = MaterialTheme.typography.bodyLarge
                                ) 
                            },
                            onClick = {
                                viewModel.updateProject(null)
                                isProjectDropdownExpanded = false
                            }
                        )

                        // Add project options
                        state.availableProjects.forEach { project ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        project.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    ) 
                                },
                                onClick = {
                                    viewModel.updateProject(project.id)
                                    isProjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            Spacer(modifier = Modifier.height(24.dp))

                // Due date picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Due Date",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            state.task.dueDate?.let { calendar.time = it }

                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)
                                    viewModel.updateDueDate(calendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            Icons.Default.DateRange, 
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            state.task.dueDate?.let {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                            } ?: "Set Due Date",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Subtasks
                Text(
                    text = "Subtasks",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    state.task.subtasks.forEach { subtask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { viewModel.toggleSubtaskCompletion(subtask.id) }
                            )

                            Text(
                                text = subtask.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            IconButton(onClick = { viewModel.removeSubtask(subtask.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add subtask
                var newSubtaskTitle by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        label = { Text("New Subtask") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                viewModel.addSubtask(newSubtaskTitle)
                                newSubtaskTitle = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = { viewModel.saveTask() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && state.task.title.isNotBlank()
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save Task")
                }
            }
        }
    }
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    IconButton(onClick = { onCheckedChange(!checked) }) {
        Icon(
            imageVector = if (checked) Icons.Default.Done else Icons.Default.Circle,
            contentDescription = if (checked) "Completed" else "Not completed",
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}
