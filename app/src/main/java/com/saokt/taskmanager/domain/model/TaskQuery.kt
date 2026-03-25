package com.saokt.taskmanager.domain.model

enum class TaskStatusFilter {
    ALL,
    OPEN,
    COMPLETED
}

enum class TaskAssignmentFilter {
    ALL,
    ASSIGNED_TO_ME,
    CREATED_BY_ME,
    UNASSIGNED
}

enum class DueDateBucket {
    ANY,
    OVERDUE,
    TODAY,
    THIS_WEEK,
    NO_DUE_DATE
}

enum class TaskSort {
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    RECENTLY_MODIFIED,
    PRIORITY_HIGH_TO_LOW,
    ALPHABETICAL
}

enum class ProjectTaskViewMode {
    LIST,
    BOARD,
    TIMELINE
}

enum class TaskListViewMode {
    LIST,
    TIMELINE
}

data class TaskListQuery(
    val status: TaskStatusFilter = TaskStatusFilter.ALL,
    val assignment: TaskAssignmentFilter = TaskAssignmentFilter.ALL,
    val priorities: Set<Priority> = emptySet(),
    val dueDate: DueDateBucket = DueDateBucket.ANY,
    val projectId: String? = null,
    val sort: TaskSort = TaskSort.DUE_DATE_ASC
) {
    fun hasActiveFilters(): Boolean {
        return status != TaskStatusFilter.ALL ||
            assignment != TaskAssignmentFilter.ALL ||
            priorities.isNotEmpty() ||
            dueDate != DueDateBucket.ANY ||
            projectId != null
    }
}
