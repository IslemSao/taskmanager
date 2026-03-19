package com.saokt.taskmanager.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add // Keep FAB icon
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WelcomeSection(
                userName = state.userName,
                openTaskCount = state.tasks.count { !it.completed },
                projectCount = state.projects.count { !it.isCompleted }
            )
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
fun WelcomeSection(
    userName: String,
    openTaskCount: Int,
    projectCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Today's workspace",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Stay on top of your next tasks, active projects, and updates from one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryPill(
                    label = "Open tasks",
                    value = openTaskCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryPill(
                    label = "Active projects",
                    value = projectCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        SectionHeader(title = "Quick Actions")

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionCard(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Tasks",
                subtitle = "Review what's next",
                onClick = onViewAllTasksClick,
                modifier = Modifier.weight(1f)
            )

            ActionCard(
                icon = Icons.Default.DateRange,
                title = "Calendar",
                subtitle = "Plan the week",
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
                subtitle = "Start something new",
                onClick = onCreateProjectClick,
                modifier = Modifier.weight(1f)
            )

            ActionCard(
                icon = Icons.Default.FolderOpen,
                title = "Projects",
                subtitle = "Track all spaces",
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
                subtitle = "See recent alerts",
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
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(118.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onActionClick)
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
        SectionHeader(
            title = "Projects",
            actionLabel = "View All",
            onActionClick = onViewAllClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (projects.isEmpty()) {
            SoftEmptyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                title = "No projects yet",
                body = "Create a project to organize tasks, members, and deadlines."
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    val accent = Color(project.color ?: MaterialTheme.colorScheme.primary.value.toInt())
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(132.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.description.ifBlank { "No description yet" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (project.isCompleted) "Completed" else "Active",
                style = MaterialTheme.typography.labelMedium,
                color = if (project.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
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
        SectionHeader(
            title = "Upcoming Tasks",
            actionLabel = "View All",
            onActionClick = onViewAllClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            SoftEmptyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                title = "No upcoming tasks",
                body = "When you add work with due dates, it will show up here."
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tasks.forEach { task ->
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

@Composable
private fun SoftEmptyCard(
    modifier: Modifier = Modifier,
    title: String,
    body: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
