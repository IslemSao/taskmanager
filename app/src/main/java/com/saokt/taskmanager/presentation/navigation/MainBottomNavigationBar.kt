package com.saokt.taskmanager.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private data class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainTabs = listOf(
    MainTab(Screen.Dashboard.route, "Home", Icons.Default.Home),
    MainTab(Screen.TaskList.route, "Tasks", Icons.AutoMirrored.Filled.List),
    MainTab(Screen.Calendar.route, "Calendar", Icons.Default.CalendarMonth),
    MainTab(Screen.ProjectList.route, "Projects", Icons.Default.FolderOpen),
    MainTab(Screen.Profile.route, "Profile", Icons.Default.Person)
)

@Composable
fun MainBottomNavigationBar(
    currentRoute: String?,
    onNavigateToTab: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        mainTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                selected = selected,
                onClick = { onNavigateToTab(tab.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
