package com.saokt.taskmanager.presentation.project.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.saokt.taskmanager.domain.model.ProjectTaskViewMode
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.EmptyStateCard
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.presentation.components.TaskFilterBar
import com.saokt.taskmanager.presentation.components.TaskItem
import com.saokt.taskmanager.presentation.components.TaskTimeline
import com.saokt.taskmanager.presentation.navigation.Screen
import com.saokt.taskmanager.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTasksScreen(
    navController: NavController,
    viewModel: ProjectTasksViewModel = hiltViewModel(),
    projectId: String
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        viewModel.loadProjectMembers(projectId)
        viewModel.loadProjectTasks(projectId)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.project?.title ?: "Project tasks",
                subtitle = "Tasks, board flow, and timeline for this project",
                onBack = { navController.navigateUp() },
                actions = {
                    IconButton(
                        onClick = {
                            state.project?.let { navController.navigate(Screen.ProjectDetail.createRoute(it.id)) }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit project")
                    }
                    IconButton(
                        onClick = {
                            val project = state.project
                            if (project != null) {
                                val memberIds = state.projectMembers.map { it.userId }
                                val participants = (memberIds + project.ownerId).distinct()
                                val currentUserId = state.currentUser?.id ?: ""
                                navController.navigate(
                                    Screen.Chat.createRoute(
                                        projectId = project.id,
                                        taskId = null,
                                        participantsCsv = participants.joinToString(","),
                                        currentUserId = currentUserId
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Open chat")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.TaskDetail.createRoute(taskId = "new", projectId = projectId))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = AppTheme.screenPadding,
                    end = AppTheme.screenPadding,
                    top = 8.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.sectionSpacing)
            ) {
                item {
                    HeroCard(
                        eyebrow = "Project view",
                        title = state.project?.title ?: "Project",
                        body = "Switch between list, board, and timeline to match how you want to plan this project today.",
                        stats = listOf(
                            "Tasks" to state.filteredTaskCount.toString(),
                            "People" to state.projectMembers.size.toString()
                        )
                    )
                }

                if (state.currentUser?.id == state.project?.ownerId) {
                    item {
                        SectionCard(title = "Owner view") {
                            Text(
                                text = "You can see and move every task in this project.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    TaskFilterBar(
                        query = state.activeQuery,
                        projects = emptyList(),
                        filteredCount = state.filteredTaskCount,
                        totalCount = state.totalTaskCount,
                        showProjectFilter = false,
                        onStatusChange = viewModel::updateStatusFilter,
                        onAssignmentChange = viewModel::updateAssignmentFilter,
                        onPriorityToggle = viewModel::togglePriority,
                        onDueDateChange = viewModel::updateDueDateFilter,
                        onProjectChange = {},
                        onSortChange = viewModel::updateSort,
                        onClearFilters = viewModel::clearFilters
                    )
                }

                item {
                    SectionCard(title = "View mode") {
                        ViewModeToggle(
                            viewMode = state.viewMode,
                            onViewModeChanged = viewModel::setViewMode
                        )
                    }
                }

                item {
                    when {
                        state.tasks.isEmpty() -> {
                            EmptyProjectTasksState(
                                hasFilters = state.hasActiveFilters,
                                isProjectOwner = state.currentUser?.id == state.project?.ownerId
                            )
                        }

                        state.viewMode == ProjectTaskViewMode.LIST -> {
                            SectionCard(title = "Task list") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.tasks.forEach { task ->
                                        TaskItem(
                                            task = task,
                                            onClick = { navController.navigate(Screen.TaskDetail.createRoute(task.id)) },
                                            onCompletionToggle = viewModel::toggleTaskCompletion,
                                            projectMembers = state.projectMembers,
                                            currentUser = state.currentUser,
                                            projects = listOfNotNull(state.project)
                                        )
                                    }
                                }
                            }
                        }

                        state.viewMode == ProjectTaskViewMode.BOARD -> {
                            SectionCard(title = "Board") {
                                ProjectBoard(
                                    state = state,
                                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                                    onMoveTask = viewModel::moveTask
                                )
                            }
                        }

                        else -> {
                            SectionCard(title = "Timeline") {
                                state.timelineRange?.let { timelineRange ->
                                    TaskTimeline(
                                        timelineRange = timelineRange,
                                        items = state.timelineItems,
                                        unscheduledTasks = state.unscheduledTasks,
                                        zoom = state.timelineZoom,
                                        onZoomChange = viewModel::setTimelineZoom,
                                        onShiftBackward = { viewModel.shiftTimelineAnchor(-7) },
                                        onShiftForward = { viewModel.shiftTimelineAnchor(7) },
                                        onJumpToToday = viewModel::jumpTimelineToToday,
                                        onTaskClick = { taskId ->
                                            navController.navigate(Screen.TaskDetail.createRoute(taskId))
                                        },
                                        onPlanTask = viewModel::planTaskOnTimeline,
                                        onRescheduleTask = viewModel::rescheduleTask,
                                        onResizeTask = viewModel::resizeTaskSchedule,
                                        secondaryLabel = { task ->
                                            state.projectMembers.firstOrNull { it.userId == task.assignedTo }?.displayName
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeToggle(
    viewMode: ProjectTaskViewMode,
    onViewModeChanged: (ProjectTaskViewMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ProjectTaskViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == viewMode,
                onClick = { onViewModeChanged(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ProjectTaskViewMode.entries.size),
                icon = {
                    Icon(
                        imageVector = when (mode) {
                            ProjectTaskViewMode.LIST -> Icons.Default.ViewAgenda
                            ProjectTaskViewMode.BOARD -> Icons.Default.Edit
                            ProjectTaskViewMode.TIMELINE -> Icons.Default.Timeline
                        },
                        contentDescription = null
                    )
                }
            ) {
                Text(
                    when (mode) {
                        ProjectTaskViewMode.LIST -> "List"
                        ProjectTaskViewMode.BOARD -> "Board"
                        ProjectTaskViewMode.TIMELINE -> "Timeline"
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectTasksState(
    hasFilters: Boolean,
    isProjectOwner: Boolean
) {
    EmptyStateCard(
        title = if (hasFilters) {
            "No tasks match the current filters"
        } else if (isProjectOwner) {
            "No tasks in this project yet"
        } else {
            "No visible tasks yet"
        },
        body = if (hasFilters) {
            "Try changing the filters or sort options."
        } else {
            "Add your first task to start planning work here."
        },
        icon = Icons.Default.ViewAgenda
    )
}

@Composable
private fun ProjectBoard(
    state: ProjectTasksState,
    onTaskClick: (String) -> Unit,
    onMoveTask: (Task, TaskStatus) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(TaskStatus.entries, key = { it.name }) { status ->
            BoardColumn(
                status = status,
                tasks = state.groupedTasks[status].orEmpty(),
                canMoveTaskById = state.canMoveTaskById,
                projectTasksState = state,
                onTaskClick = onTaskClick,
                onMoveTask = onMoveTask
            )
        }
    }
}

@Composable
private fun BoardColumn(
    status: TaskStatus,
    tasks: List<Task>,
    canMoveTaskById: Map<String, Boolean>,
    projectTasksState: ProjectTasksState,
    onTaskClick: (String) -> Unit,
    onMoveTask: (Task, TaskStatus) -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FilterChip(selected = false, onClick = {}, label = { Text(tasks.size.toString()) })
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(min = 120.dp)
            ) {
                tasks.forEach { task ->
                    BoardTaskCard(
                        task = task,
                        canMove = canMoveTaskById[task.id] == true,
                        assigneeName = projectTasksState.projectMembers.firstOrNull { it.userId == task.assignedTo }?.displayName,
                        onTaskClick = { onTaskClick(task.id) },
                        onMoveTask = onMoveTask
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardTaskCard(
    task: Task,
    canMove: Boolean,
    assigneeName: String?,
    onTaskClick: () -> Unit,
    onMoveTask: (Task, TaskStatus) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Card(
        onClick = onTaskClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Move task")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        TaskStatus.entries.filter { it != task.status }.forEach { targetStatus ->
                            DropdownMenuItem(
                                text = { Text("Move to ${targetStatus.displayName()}") },
                                enabled = canMove,
                                onClick = {
                                    onMoveTask(task, targetStatus)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Priority: ${task.priority.name.lowercase().replaceFirstChar(Char::titlecase)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            assigneeName?.let {
                Text(
                    text = "Assignee: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            task.dueDate?.let {
                Text(
                    text = "Due ${formatter.format(it)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.TODO -> "Todo"
    TaskStatus.IN_PROGRESS -> "In progress"
    TaskStatus.DONE -> "Done"
}
