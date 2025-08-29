package com.saokt.taskmanager.presentation.project.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.saokt.taskmanager.presentation.components.TaskItem
import com.saokt.taskmanager.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTasksScreen(
    navController: NavController,
    viewModel: ProjectTasksViewModel = hiltViewModel(),
    projectId: String
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        viewModel.loadProjectMembers(projectId)
        viewModel.loadProjectTasks(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.project?.title ?: "Project Tasks") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            state.project?.let { project ->
                                navController.navigate(Screen.ProjectDetail.createRoute(project.id))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Project")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.TaskDetail.createRoute("new"))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Project owner banner
            val currentUser = state.currentUser
            val project = state.project
            val isProjectOwner = currentUser != null &&
                project != null &&
                project.ownerId == currentUser.id

            if (isProjectOwner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Viewing all tasks as project owner",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Tasks list
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (state.tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isProjectOwner) {
                                "No tasks in this project yet"
                            } else {
                                "No tasks assigned to you in this project"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Tap the + button to add a new task",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tasks) { task ->
                        TaskItem(
                            task = task,
                            onClick = {
                                navController.navigate(Screen.TaskDetail.createRoute(task.id))
                            },
                            onCompletionToggle = { taskToToggle ->
                                viewModel.toggleTaskCompletion(taskToToggle)
                            },
                            projectMembers = state.projectMembers,
                            currentUser = state.currentUser,
                            projects = listOfNotNull(state.project)
                        )
                    }
                }
            }
        }
    }
}
