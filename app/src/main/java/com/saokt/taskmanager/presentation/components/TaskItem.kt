package com.saokt.taskmanager.presentation.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.Task
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onCompletionToggle: (Task) -> Unit,
    projectMembers: List<com.saokt.taskmanager.domain.model.ProjectMember> = emptyList(),
    currentUser: com.saokt.taskmanager.domain.model.User? = null,
    projects: List<com.saokt.taskmanager.domain.model.Project> = emptyList()
) {
    val isAssignedToMe = currentUser != null && task.assignedTo == currentUser.id && task.createdBy != currentUser.id
    val isCreatedByMe = currentUser != null && task.createdBy == currentUser.id
    val isOwner = currentUser != null && projects.any { it.ownerId == currentUser.id && it.id == task.projectId }
    val assignee = projectMembers.find { it.userId == task.assignedTo }
    val creator = projectMembers.find { it.userId == task.createdBy }
    val assigner = projectMembers.find { it.userId == task.assignedBy }
    Log.d("TaskItemDebug", "taskId=${task.id}, title=${task.title}, assignedTo=${task.assignedTo}, createdBy=${task.createdBy}, assignedBy=${task.assignedBy}, currentUser=${currentUser?.id}, isAssignedToMe=$isAssignedToMe, isCreatedByMe=$isCreatedByMe, isOwner=$isOwner, assignee=${assignee?.displayName}, creator=${creator?.displayName}, assigner=${assigner?.displayName}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed)
                MaterialTheme.colorScheme.surfaceVariant
            else if (isAssignedToMe)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        when (task.priority) {
                            Priority.HIGH -> MaterialTheme.colorScheme.error
                            Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            Priority.LOW -> MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                        color = if (task.completed)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (isAssignedToMe) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assigned to you",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (task.completed)
                            MaterialTheme.colorScheme.outline
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Assignment info
                if (assignee != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Assigned to",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Assigned to ${assignee.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (assigner != null && assigner.userId != assignee.userId) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "by ${assigner.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                if (creator != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Created by ${creator.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(task.dueDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(
                imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = if (task.completed) "Completed" else "Not completed",
                tint = if (task.completed)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onCompletionToggle(task) }
                    .padding(2.dp)
            )
        }
    }
}