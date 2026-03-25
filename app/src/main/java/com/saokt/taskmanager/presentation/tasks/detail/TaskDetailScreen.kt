package com.saokt.taskmanager.presentation.tasks.detail

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.TaskType
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.InfoChip
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel,
    taskId: String,
    initialProjectId: String? = null
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(taskId, initialProjectId) {
        viewModel.loadTask(taskId, initialProjectId)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.isTaskSaved) {
        if (state.isTaskSaved) navController.navigateUp()
    }

    if (state.isLoading) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = if (state.isNewTask) "New task" else "Edit task",
                    subtitle = "Organize the task details, assignment, and schedule",
                    onBack = { navController.navigateUp() }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    var newSubtaskTitle by remember { mutableStateOf("") }

    fun saveTaskWithPendingSubtask() {
        if (newSubtaskTitle.isNotBlank()) {
            viewModel.addSubtask(newSubtaskTitle)
            newSubtaskTitle = ""
        }
        viewModel.saveTask()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.isNewTask) "New task" else "Edit task",
                subtitle = "Set status, ownership, and schedule with less friction",
                onBack = { navController.navigateUp() },
                actions = {
                    IconButton(
                        onClick = { saveTaskWithPendingSubtask() },
                        enabled = !state.isSaving && state.task.title.isNotBlank(),
                        modifier = Modifier.testTag(TaskDetailTestTags.TOP_SAVE_BUTTON)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                    IconButton(
                        onClick = {
                            val task = state.task
                            val projectId = task.projectId
                            if (projectId != null) {
                                val assigneeId = task.assignedTo
                                val creatorId = task.createdBy
                                val ownerId = state.availableProjects.find { it.id == projectId }?.ownerId
                                val currentUserId = state.currentUser?.id ?: ""
                                val participants = listOfNotNull(assigneeId, creatorId, ownerId, currentUserId).distinct()
                                if (participants.isNotEmpty()) {
                                    val csv = participants.joinToString(",")
                                    navController.navigate(
                                        com.saokt.taskmanager.presentation.navigation.Screen.Chat.createRoute(
                                            projectId = projectId,
                                            taskId = task.id,
                                            participantsCsv = csv,
                                            currentUserId = currentUserId
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Open chat")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
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
                    eyebrow = "Task editor",
                    title = if (state.task.title.isBlank()) "Untitled task" else state.task.title,
                    body = "Group task details into clear sections so it is easier to plan, assign, and follow up.",
                    stats = listOf(
                        "Status" to state.task.status.displayName(),
                        "Priority" to state.task.priority.name.lowercase().replaceFirstChar(Char::titlecase)
                    )
                )
            }

            item {
                SectionCard(title = "Basics") {
                    OutlinedTextField(
                        value = state.task.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("Task title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TaskDetailTestTags.TITLE_FIELD)
                    )
                    OutlinedTextField(
                        value = state.task.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TaskDetailTestTags.DESCRIPTION_FIELD),
                        minLines = 4
                    )
                }
            }

            item {
                SectionCard(title = "Workflow") {
                    Text("Status", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TaskStatus.entries.forEachIndexed { index, status ->
                            SegmentedButton(
                                selected = state.task.status == status,
                                onClick = { viewModel.updateStatus(status) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TaskStatus.entries.size)
                            ) {
                                Text(status.displayName())
                            }
                        }
                    }

                    Text("Type", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TaskType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = state.task.type == type,
                                onClick = { viewModel.updateTaskType(type) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TaskType.entries.size)
                            ) {
                                Text(if (type == TaskType.TASK) "Task" else "Milestone")
                            }
                        }
                    }

                    Text("Priority", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        Priority.entries.forEach { priority ->
                            SegmentedButton(
                                selected = state.task.priority == priority,
                                onClick = { viewModel.updatePriority(priority) },
                                shape = SegmentedButtonDefaults.itemShape(index = priority.ordinal, count = Priority.entries.size),
                                modifier = Modifier.testTag(
                                    when (priority) {
                                        Priority.LOW -> TaskDetailTestTags.PRIORITY_LOW
                                        Priority.MEDIUM -> TaskDetailTestTags.PRIORITY_MEDIUM
                                        Priority.HIGH -> TaskDetailTestTags.PRIORITY_HIGH
                                    }
                                )
                            ) {
                                Text(priority.name.lowercase().replaceFirstChar(Char::titlecase))
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Project and assignment") {
                    var isProjectDropdownExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { isProjectDropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TaskDetailTestTags.PROJECT_DROPDOWN)
                        ) {
                            Text(state.availableProjects.find { it.id == state.task.projectId }?.title ?: "No project")
                        }
                        DropdownMenu(
                            expanded = isProjectDropdownExpanded,
                            onDismissRequest = { isProjectDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No project") },
                                onClick = {
                                    viewModel.updateProject(null)
                                    isProjectDropdownExpanded = false
                                }
                            )
                            state.availableProjects.forEach { project ->
                                DropdownMenuItem(
                                    text = { Text(project.title) },
                                    onClick = {
                                        viewModel.updateProject(project.id)
                                        isProjectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (state.task.projectId != null && state.isProjectOwner) {
                        var isAssigneeDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { isAssigneeDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                                Text(state.projectMembers.find { it.userId == state.task.assignedTo }?.displayName ?: "Unassigned")
                            }
                            DropdownMenu(
                                expanded = isAssigneeDropdownExpanded,
                                onDismissRequest = { isAssigneeDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Unassigned") },
                                    onClick = {
                                        viewModel.updateAssignee(null)
                                        isAssigneeDropdownExpanded = false
                                    }
                                )
                                state.projectMembers.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.displayName) },
                                        onClick = {
                                            viewModel.updateAssignee(member.userId)
                                            isAssigneeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    state.task.assignedTo?.let { assigneeId ->
                        state.projectMembers.find { it.userId == assigneeId }?.let {
                            InfoChip(label = "Assigned to ${it.displayName}")
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Schedule") {
                    DateFieldRow(
                        label = "Start date",
                        date = state.task.startDate,
                        buttonTag = TaskDetailTestTags.START_DATE_BUTTON,
                        context = context,
                        onDateSelected = viewModel::updateStartDate
                    )
                    DateFieldRow(
                        label = if (state.task.type == TaskType.MILESTONE) "Milestone date" else "Due date",
                        date = state.task.dueDate,
                        buttonTag = TaskDetailTestTags.DUE_DATE_BUTTON,
                        context = context,
                        onDateSelected = viewModel::updateDueDate
                    )
                }
            }

            item {
                SectionCard(title = "Subtasks") {
                    if (state.task.subtasks.isEmpty()) {
                        Text(
                            text = "Break the task into smaller steps to make progress easier to track.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.task.subtasks.forEach { subtask ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                        Icon(Icons.Default.Delete, contentDescription = "Delete subtask")
                                    }
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            label = { Text("New subtask") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(TaskDetailTestTags.NEW_SUBTASK_FIELD)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = {
                                if (newSubtaskTitle.isNotBlank()) {
                                    viewModel.addSubtask(newSubtaskTitle)
                                    newSubtaskTitle = ""
                                }
                            },
                            modifier = Modifier.testTag(TaskDetailTestTags.ADD_SUBTASK_BUTTON)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { saveTaskWithPendingSubtask() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag(TaskDetailTestTags.SAVE_BUTTON),
                    enabled = !state.isSaving && state.task.title.isNotBlank()
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save task")
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

object TaskDetailTestTags {
    const val TITLE_FIELD = "task_detail_title_field"
    const val DESCRIPTION_FIELD = "task_detail_description_field"
    const val START_DATE_BUTTON = "task_detail_start_date_button"
    const val DUE_DATE_BUTTON = "task_detail_due_date_button"
    const val PRIORITY_LOW = "task_detail_priority_low"
    const val PRIORITY_MEDIUM = "task_detail_priority_medium"
    const val PRIORITY_HIGH = "task_detail_priority_high"
    const val PROJECT_DROPDOWN = "task_detail_project_dropdown"
    const val NEW_SUBTASK_FIELD = "task_detail_new_subtask_field"
    const val ADD_SUBTASK_BUTTON = "task_detail_add_subtask_button"
    const val SAVE_BUTTON = "task_detail_save_button"
    const val TOP_SAVE_BUTTON = "task_detail_top_save_button"
}

@Composable
private fun DateFieldRow(
    label: String,
    date: java.util.Date?,
    buttonTag: String,
    context: android.content.Context,
    onDateSelected: (java.util.Date?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = date?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it) } ?: "Not set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            onClick = {
                val calendar = Calendar.getInstance()
                date?.let { calendar.time = it }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.testTag(buttonTag)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Select date")
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
            Text("Choose")
        }
    }
}

private fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.TODO -> "Todo"
    TaskStatus.IN_PROGRESS -> "In progress"
    TaskStatus.DONE -> "Done"
}
