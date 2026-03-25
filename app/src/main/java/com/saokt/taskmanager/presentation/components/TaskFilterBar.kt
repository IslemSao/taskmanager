package com.saokt.taskmanager.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saokt.taskmanager.domain.model.DueDateBucket
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.TaskAssignmentFilter
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.TaskSort
import com.saokt.taskmanager.domain.model.TaskStatusFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskFilterBar(
    query: TaskListQuery,
    projects: List<Project>,
    filteredCount: Int,
    totalCount: Int,
    showProjectFilter: Boolean,
    onStatusChange: (TaskStatusFilter) -> Unit,
    onAssignmentChange: (TaskAssignmentFilter) -> Unit,
    onPriorityToggle: (Priority) -> Unit,
    onDueDateChange: (DueDateBucket) -> Unit,
    onProjectChange: (String?) -> Unit,
    onSortChange: (TaskSort) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Showing $filteredCount of $totalCount tasks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (query.hasActiveFilters()) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear filters")
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnumFilterChip(
                label = "Status",
                valueLabel = query.status.displayName(),
                options = TaskStatusFilter.entries,
                selectedOption = query.status,
                onOptionSelected = onStatusChange
            )
            EnumFilterChip(
                label = "Assignment",
                valueLabel = query.assignment.displayName(),
                options = TaskAssignmentFilter.entries,
                selectedOption = query.assignment,
                onOptionSelected = onAssignmentChange
            )
            EnumFilterChip(
                label = "Due",
                valueLabel = query.dueDate.displayName(),
                options = DueDateBucket.entries,
                selectedOption = query.dueDate,
                onOptionSelected = onDueDateChange
            )
            EnumFilterChip(
                label = "Sort",
                valueLabel = query.sort.displayName(),
                options = TaskSort.entries,
                selectedOption = query.sort,
                onOptionSelected = onSortChange
            )
            if (showProjectFilter) {
                ProjectFilterChip(
                    currentProjectId = query.projectId,
                    projects = projects,
                    onProjectChange = onProjectChange
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Priority.entries.forEach { priority ->
                FilterChip(
                    selected = priority in query.priorities,
                    onClick = { onPriorityToggle(priority) },
                    label = { Text(priority.name.lowercase().replaceFirstChar(Char::titlecase)) }
                )
            }
        }
    }
}

@Composable
private fun <T> EnumFilterChip(
    label: String,
    valueLabel: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text("$label: $valueLabel") }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString().replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProjectFilterChip(
    currentProjectId: String?,
    projects: List<Project>,
    onProjectChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = projects.firstOrNull { it.id == currentProjectId }?.title ?: "All projects"
    androidx.compose.foundation.layout.Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text("Project: $currentLabel") }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All projects") },
                onClick = {
                    onProjectChange(null)
                    expanded = false
                }
            )
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.title) },
                    onClick = {
                        onProjectChange(project.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun TaskStatusFilter.displayName(): String = when (this) {
    TaskStatusFilter.ALL -> "All"
    TaskStatusFilter.OPEN -> "Open"
    TaskStatusFilter.COMPLETED -> "Completed"
}

private fun TaskAssignmentFilter.displayName(): String = when (this) {
    TaskAssignmentFilter.ALL -> "All"
    TaskAssignmentFilter.ASSIGNED_TO_ME -> "Assigned to me"
    TaskAssignmentFilter.CREATED_BY_ME -> "Created by me"
    TaskAssignmentFilter.UNASSIGNED -> "Unassigned"
}

private fun DueDateBucket.displayName(): String = when (this) {
    DueDateBucket.ANY -> "Any"
    DueDateBucket.OVERDUE -> "Overdue"
    DueDateBucket.TODAY -> "Today"
    DueDateBucket.THIS_WEEK -> "This week"
    DueDateBucket.NO_DUE_DATE -> "No due date"
}

private fun TaskSort.displayName(): String = when (this) {
    TaskSort.DUE_DATE_ASC -> "Due date asc"
    TaskSort.DUE_DATE_DESC -> "Due date desc"
    TaskSort.RECENTLY_MODIFIED -> "Recently modified"
    TaskSort.PRIORITY_HIGH_TO_LOW -> "Priority"
    TaskSort.ALPHABETICAL -> "Alphabetical"
}
