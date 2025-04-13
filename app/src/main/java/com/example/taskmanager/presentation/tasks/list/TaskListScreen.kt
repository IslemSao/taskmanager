package com.example.taskmanager.presentation.tasks.list

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.presentation.components.TaskItem
import com.example.taskmanager.presentation.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: TaskListViewModel
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
            TopAppBar(
                title = { Text("Tasks") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.TaskDetail.createRoute())
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
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
            TaskList(
                tasks = state.tasks,
                contentPadding = padding,
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onTaskDelete = { taskId ->
                    viewModel.deleteTask(taskId)
                },
                snackbarHostState = snackbarHostState,
                onCompletionToggle = { task ->
                    viewModel.toggleTaskCompletion(task)
                },
                projects = state.projects
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskList(
    tasks: List<Task>,
    contentPadding: PaddingValues,
    projects: List<Project>,
    onTaskClick: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onCompletionToggle: (Task) -> Unit
) {
    val listState = rememberLazyListState()
    var itemsState by remember { mutableStateOf(tasks) }
    val scope = rememberCoroutineScope()

    // Track visible items to handle redisplay after swipe cancellation
    var visibleItems by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // Track items being processed for deletion
    var processingDeletion by remember { mutableStateOf<String?>(null) }

    // Initialize visible state for all tasks
    LaunchedEffect(tasks) {
        itemsState = tasks
        visibleItems = tasks.associate { it.id to true }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = itemsState,
            key = { it.id }
        ) { task ->
            val currentlyVisible = visibleItems[task.id] ?: true

            // Create a dismissState for each item
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    // Only show the snackbar confirmation dialog if swiped to delete
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                        // Set the current task as being processed for deletion
                        processingDeletion = task.id

                        // Handle the confirmation in a coroutine
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Delete '${task.title}'?",
                                actionLabel = "Delete",
                                withDismissAction = true,
                                duration = SnackbarDuration.Long
                            )

                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    // User confirmed deletion
                                    visibleItems = visibleItems.toMutableMap().apply {
                                        put(task.id, false)
                                    }
                                    // Delay actual deletion to allow animation to complete
                                    delay(300)
                                    onTaskDelete(task.id)
                                }
                                SnackbarResult.Dismissed -> {
                                    // User cancelled - we'll handle reset in LaunchedEffect below
                                    processingDeletion = null
                                }
                            }
                        }
                        // Return false to prevent automatic dismissal
                        false
                    } else {
                        // Allow other swipe actions to proceed normally
                        false
                    }
                },
                // Increase swipe resistance
                positionalThreshold = { totalDistance -> totalDistance * 0.6f },
            )

            // Handle dismissState reset when cancellation happens
            LaunchedEffect(processingDeletion) {
                if (processingDeletion == null && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    // Reset the dismiss state when processingDeletion is cleared (cancel case)
                    dismissState.reset()
                }
            }

            AnimatedVisibility(
                visible = currentlyVisible,
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false, // Only allow right-to-left swipe
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
                        Log.d("bombardiro", "lista ${projects}")
                        val borderColor = projects.find { it.id == task.projectId }?.color ?: 0x000
                        TaskItem(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onCompletionToggle = { onCompletionToggle(task) },
                            borderColor = Color(borderColor)
                        )
                    }
                )
            }
        }
    }
}