package com.saokt.taskmanager.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Keep FAB icon
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.Task
// import com.saokt.taskmanager.presentation.authentication.signin.SignInViewModel // No longer needed here
import com.saokt.taskmanager.presentation.components.TaskItem // Assuming this exists
import com.saokt.taskmanager.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel // Only DashboardViewModel is needed now
    // signInViewModel: SignInViewModel // Removed
) {
    // Observe the state directly from DashboardViewModel
    // This state is now updated based on Room changes driven by Firestore listeners
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Removed LaunchedEffect based on isSignedIn ---
    // The ViewModel now handles triggering data load internally based on auth state.
    // LaunchedEffect(key1 = isSignedIn) { ... } // Removed

    // Effect to show snackbar errors remains the same
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError() // Clear error after showing
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate to create a new task (or project?)
                    navController.navigate(Screen.TaskDetail.createRoute()) // Example
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add") // Generic Add icon
            }
        }    ) { padding ->
        // Show loading indicator based on ViewModel's state
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
            // Display content when not loading
            // Pass the state directly, which contains data from Room
            DashboardContent(
                state = state,
                contentPadding = padding,
                onCompletionToggle = { task ->
                    viewModel.toggleTaskCompletion(task)
                },
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                },
                onViewAllTasksClick = {
                    navController.navigate(Screen.TaskList.route)
                },
                onViewAllProjectsClick = {
                    navController.navigate(Screen.ProjectList.route)
                },
                onViewCalendarClick = {
                    navController.navigate(Screen.Calendar.route)
                },
                onCreateProjectClick = {
                    navController.navigate(Screen.ProjectDetail.createRoute())
                },
                onViewNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                }
                // Pass refresh lambda if you added a refresh mechanism
                // onRefresh = { viewModel.refreshData() }
            )
        }
    }
}

// --- DashboardContent and sub-composables remain unchanged ---
// They correctly read data from the DashboardState provided.

@Composable
fun DashboardContent(
    state: DashboardState,
    contentPadding: PaddingValues,
    onTaskClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onViewAllTasksClick: () -> Unit,
    onViewAllProjectsClick: () -> Unit,
    onViewCalendarClick: () -> Unit,
    onCreateProjectClick: () -> Unit,
    onViewNotificationsClick: () -> Unit,
    onCompletionToggle: (Task) -> Unit
    // onRefresh: () -> Unit // Add if implementing SwipeRefresh or similar
) {
    // Consider adding SwipeRefresh here if desired
    /*
     val pullRefreshState = rememberPullRefreshState(refreshing = state.isLoading, onRefresh = onRefresh)
     Box(Modifier.pullRefresh(pullRefreshState)) {
         LazyColumn(...) // Your existing LazyColumn
         PullRefreshIndicator(state.isLoading, pullRefreshState, Modifier.align(Alignment.TopCenter))
     }
    */

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(bottom = 16.dp), // Add padding at the bottom too if needed
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WelcomeSection(userName = state.userName)
        }

        item {
            QuickActionsSection(
                onViewAllTasksClick = onViewAllTasksClick,
                onViewAllProjectsClick = onViewAllProjectsClick,
                onViewCalendarClick = onViewCalendarClick,
                onCreateProjectClick = onCreateProjectClick,
                onViewNotificationsClick = onViewNotificationsClick
            )
        }

        // Only show sections if data is available, or show empty states
        if (state.projects.isNotEmpty() || !state.isLoading) { // Show section even if empty after loading
            item {
                ProjectsSection(
                    projects = state.projects.take(5), // Example: limit items on dashboard
                    onProjectClick = onProjectClick,
                    onViewAllClick = onViewAllProjectsClick
                )
            }
        }

        if (state.tasks.isNotEmpty() || !state.isLoading) { // Show section even if empty after loading
            item {
                UpcomingTasksSection(
                    // Show all uncompleted tasks, sorted by due date (null dates will appear last)
                    tasks = state.tasks
                        .filter { !it.completed }
                        .sortedWith(compareBy<Task> { it.dueDate == null }.thenBy { it.dueDate })
                        .take(5),
                    onTaskClick = onTaskClick,
                    onViewAllClick = onViewAllTasksClick,
                    onCompletionToggle = { onCompletionToggle(it) },
                    projects = state.projects // Pass projects for color lookup
                )
            }
        }

        // You could add a spacer at the end if needed for FAB overlap etc.
        // item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun WelcomeSection(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onViewAllTasksClick: () -> Unit,
    onViewAllProjectsClick: () -> Unit,
    onViewCalendarClick: () -> Unit,
    onCreateProjectClick: () -> Unit,
    onViewNotificationsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionCard(
                icon = Icons.Default.List,
                title = "Tasks",
                onClick = onViewAllTasksClick,
                modifier = Modifier.weight(1f)
            )

            ActionCard(
                icon = Icons.Default.DateRange,
                title = "Calendar",
                onClick = onViewCalendarClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Create,
                title = "New Project",
                onClick = onCreateProjectClick,
                modifier = Modifier.weight(1f)
            )

            ActionCard(
                icon = Icons.Default.FolderOpen,
                title = "Projects",
                onClick = onViewAllProjectsClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                onClick = onViewNotificationsClick,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ProjectsSection(
    projects: List<Project>,
    onProjectClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "View All",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No projects yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectClick(project.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpcomingTasksSection(
    tasks: List<Task>,
    projects:List<Project>,
    onTaskClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onCompletionToggle: (Task) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Tasks",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "View All",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming tasks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tasks.forEach { task ->
                    //get the project color
                    val color = projects.find { it.id == task.projectId }?.color ?: 0x000000
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onCompletionToggle = { onCompletionToggle(task) },
                    )
                }
            }
        }
    }
}
