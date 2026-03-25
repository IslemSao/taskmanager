package com.saokt.taskmanager.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

object TaskQueryEngine {
    fun apply(
        tasks: List<Task>,
        query: TaskListQuery,
        currentUserId: String?,
        clock: Clock = Clock.systemDefaultZone()
    ): List<Task> {
        val today = LocalDate.now(clock)
        return tasks
            .asSequence()
            .map(Task::canonicalizedStatus)
            .filter { task -> matchesStatus(task, query.status) }
            .filter { task -> matchesAssignment(task, query.assignment, currentUserId) }
            .filter { task -> matchesPriority(task, query.priorities) }
            .filter { task -> matchesDueDate(task, query.dueDate, today, clock.zone) }
            .filter { task -> query.projectId == null || task.projectId == query.projectId }
            .sortedWith(comparatorFor(query.sort))
            .toList()
    }

    fun groupByStatus(tasks: List<Task>): Map<TaskStatus, List<Task>> {
        val normalized = tasks.map(Task::canonicalizedStatus)
        val ordered = listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE)
        return ordered.associateWith { status ->
            normalized
                .filter { it.status == status }
                .sortedWith(comparatorFor(TaskSort.DUE_DATE_ASC))
        }
    }

    private fun matchesStatus(task: Task, filter: TaskStatusFilter): Boolean {
        return when (filter) {
            TaskStatusFilter.ALL -> true
            TaskStatusFilter.OPEN -> task.status != TaskStatus.DONE
            TaskStatusFilter.COMPLETED -> task.status == TaskStatus.DONE
        }
    }

    private fun matchesAssignment(task: Task, filter: TaskAssignmentFilter, currentUserId: String?): Boolean {
        return when (filter) {
            TaskAssignmentFilter.ALL -> true
            TaskAssignmentFilter.ASSIGNED_TO_ME -> currentUserId != null && task.assignedTo == currentUserId
            TaskAssignmentFilter.CREATED_BY_ME -> currentUserId != null && task.createdBy == currentUserId
            TaskAssignmentFilter.UNASSIGNED -> task.assignedTo.isNullOrBlank()
        }
    }

    private fun matchesPriority(task: Task, priorities: Set<Priority>): Boolean {
        return priorities.isEmpty() || task.priority in priorities
    }

    private fun matchesDueDate(task: Task, bucket: DueDateBucket, today: LocalDate, zoneId: ZoneId): Boolean {
        val dueDate = task.dueDate?.toLocalDate(zoneId)
        return when (bucket) {
            DueDateBucket.ANY -> true
            DueDateBucket.NO_DUE_DATE -> dueDate == null
            DueDateBucket.OVERDUE -> dueDate != null && dueDate.isBefore(today)
            DueDateBucket.TODAY -> dueDate == today
            DueDateBucket.THIS_WEEK -> dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(6))
        }
    }

    private fun comparatorFor(sort: TaskSort): Comparator<Task> {
        return when (sort) {
            TaskSort.DUE_DATE_ASC -> compareBy<Task> { it.dueDate == null }
                .thenBy { it.dueDate }
                .thenByDescending { it.priority.ordinal }
                .thenBy { it.title.lowercase() }

            TaskSort.DUE_DATE_DESC -> compareByDescending<Task> { it.dueDate ?: Date(Long.MIN_VALUE) }
                .thenByDescending { it.priority.ordinal }
                .thenBy { it.title.lowercase() }

            TaskSort.RECENTLY_MODIFIED -> compareByDescending<Task> { it.modifiedAt }
                .thenBy { it.title.lowercase() }

            TaskSort.PRIORITY_HIGH_TO_LOW -> compareByDescending<Task> { it.priority.ordinal }
                .thenBy { it.dueDate == null }
                .thenBy { it.dueDate }
                .thenBy { it.title.lowercase() }

            TaskSort.ALPHABETICAL -> compareBy<Task> { it.title.lowercase() }
                .thenBy { it.dueDate == null }
                .thenBy { it.dueDate }
        }
    }
}

private fun Date.toLocalDate(zoneId: ZoneId): LocalDate =
    toInstant().atZone(zoneId).toLocalDate()
