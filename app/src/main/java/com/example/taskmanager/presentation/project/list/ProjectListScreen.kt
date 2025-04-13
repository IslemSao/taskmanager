package com.example.taskmanager.presentation.project.list

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.presentation.components.TaskItem
import com.example.taskmanager.presentation.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    navController: NavController,
    viewModel: ProjectListViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Track which projects are expanded
    val expandedProjects = remember { mutableStateMapOf<String, Boolean>() }

    // Track projects being swiped
    var swipedProjectId by remember { mutableStateOf<String?>(null) }

    // Get tasks for each project
    val projectTasks = remember(state.projects, state.tasks) {
        state.projects.associate { project ->
            project.id to state.tasks.filter { it.projectId == project.id }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
        viewModel.loadTasks()
        viewModel.loadMembers()
    }

    // Handle errors
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.ProjectDetail.createRoute())
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
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
        } else if (state.projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No projects yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.projects,
                    key = { it.id }
                ) { project ->
                    // Track if we need to reset the dismiss state
                    var shouldResetDismissState by remember { mutableStateOf(false) }

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                swipedProjectId = project.id
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Delete '${project.title}'?",
                                        actionLabel = "Delete",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Long
                                    )

                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.deleteProject(project.id)
                                        }
                                        SnackbarResult.Dismissed -> {
                                            shouldResetDismissState = true
                                        }
                                    }
                                    swipedProjectId = null
                                }
                                false
                            } else {
                                false
                            }
                        },
                        positionalThreshold = { totalDistance -> totalDistance * 0.6f }
                    )

                    // Reset dismiss state if needed
                    LaunchedEffect(shouldResetDismissState) {
                        if (shouldResetDismissState) {
                            dismissState.reset()
                            shouldResetDismissState = false
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val swipeProgress = when {
                                dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> {
                                    dismissState.progress
                                }
                                else -> 0f
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = swipeProgress))
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.alpha(swipeProgress)
                                )
                            }
                        },
                        content = {
                            AnimatedVisibility(
                                visible = state.projects.any { it.id == project.id },
                                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
                            ) {
                                ProjectCard(
                                    project = project,
                                    onClick = { navController.navigate(Screen.ProjectDetail.createRoute(project.id)) },
                                    onToggleTasks = {
                                        expandedProjects[project.id] = !(expandedProjects[project.id] ?: false)
                                    },
                                    showTasks = expandedProjects[project.id] ?: false,
                                    tasks = projectTasks[project.id],
                                    members = state.members.filter { it.projectId == project.id }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onToggleTasks: () -> Unit,
    showTasks: Boolean,
    tasks: List<Task>?,
    members: List<ProjectMember>
) {
    val projectColor = Color(project.color ?: 0x000)  // Convert stored color int to Compose Color

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(
                width = 2.dp,
                color = projectColor.copy(alpha = 0.3f), // Subtle border
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column {
            // Header with color indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(projectColor.copy(alpha = 0.1f)) // Very subtle background
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(projectColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleTasks) {
                    Icon(
                        imageVector = if (showTasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showTasks) "Hide tasks" else "Show tasks"
                    )
                }
            }

            // Members section
            if (members.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Members header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Team Members",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${members.size} ${if (members.size == 1) "member" else "members"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Members list
                    members.forEach { member ->
                        MemberRow(
                            member = member,
                            projectColor = projectColor
                        )
                    }
                }

                // Add a divider between members and tasks
                Divider(
                    color = projectColor.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
            }

            // Task list section
            AnimatedVisibility(visible = showTasks) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (tasks.isNullOrEmpty()) {
                        Text(
                            text = "No tasks in this project",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        tasks.forEach { task ->
                            TaskItem(
                                task = task,
                                onClick = { onClick() },
                                onCompletionToggle = { /* Handle completion toggle */ },
                                borderColor = projectColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberRow(
    member: ProjectMember,
    projectColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with initial
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(projectColor.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = projectColor.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Text(
                text = member.displayName.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Member info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Role chip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (member.role.toString().lowercase()) {
                        "admin" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        "owner" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = member.role.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = when (member.role.toString().lowercase()) {
                            "admin" -> MaterialTheme.colorScheme.primary
                            "owner" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }

            Text(
                text = member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}