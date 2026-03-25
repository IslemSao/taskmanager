package com.saokt.taskmanager.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

enum class TaskType {
    TASK,
    MILESTONE
}

enum class TimelineZoom {
    DAY,
    WEEK,
    MONTH
}

enum class TimelineEdge {
    START,
    END
}

data class TimelineRange(
    val start: LocalDate,
    val end: LocalDate
) {
    val totalDays: Int = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

data class TimelineItem(
    val task: Task,
    val start: LocalDate,
    val end: LocalDate,
    val isMilestone: Boolean,
    val canEditSchedule: Boolean
) {
    val spanDays: Int = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

data class TaskTimeline(
    val range: TimelineRange,
    val items: List<TimelineItem>,
    val unscheduledTasks: List<Task>
)

object TaskTimelineEngine {
    fun build(
        tasks: List<Task>,
        zoom: TimelineZoom,
        anchorDate: LocalDate?,
        canEditSchedule: (Task) -> Boolean,
        clock: Clock = Clock.systemDefaultZone(),
        zoneId: ZoneId = clock.zone
    ): TaskTimeline {
        val today = LocalDate.now(clock)
        val normalizedTasks = tasks.map(Task::canonicalized).sortedBy { it.title.lowercase() }
        val items = normalizedTasks.mapNotNull { task ->
            val bounds = scheduleBounds(task, zoneId) ?: return@mapNotNull null
            TimelineItem(
                task = task,
                start = bounds.first,
                end = bounds.second,
                isMilestone = task.type == TaskType.MILESTONE,
                canEditSchedule = canEditSchedule(task)
            )
        }
        val unscheduled = normalizedTasks.filter { scheduleBounds(it, zoneId) == null }
        val range = buildRange(
            items = items,
            zoom = zoom,
            anchorDate = anchorDate ?: today,
            today = today
        )
        return TaskTimeline(
            range = range,
            items = items,
            unscheduledTasks = unscheduled
        )
    }

    fun shiftTask(task: Task, deltaDays: Long, zoneId: ZoneId = ZoneId.systemDefault()): Task {
        val normalized = task.canonicalized()
        val start = normalized.startDate
        val end = normalized.dueDate
        return when {
            start == null && end == null -> {
                val target = LocalDate.now(zoneId).plusDays(deltaDays).toDate(zoneId)
                normalized.copy(startDate = target, dueDate = target).canonicalized()
            }
            else -> normalized.copy(
                startDate = start?.shiftDays(deltaDays, zoneId),
                dueDate = end?.shiftDays(deltaDays, zoneId)
            ).canonicalized()
        }
    }

    fun resizeTask(
        task: Task,
        edge: TimelineEdge,
        deltaDays: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Task {
        val normalized = task.canonicalized()
        if (normalized.type == TaskType.MILESTONE) {
            return shiftTask(normalized, deltaDays, zoneId)
        }

        val effectiveStart = normalized.startDate ?: normalized.dueDate
        val effectiveEnd = normalized.dueDate ?: normalized.startDate
        if (effectiveStart == null && effectiveEnd == null) {
            val target = LocalDate.now(zoneId).plusDays(deltaDays).toDate(zoneId)
            return normalized.copy(startDate = target, dueDate = target).canonicalized()
        }

        val updatedStart = when (edge) {
            TimelineEdge.START -> (effectiveStart ?: effectiveEnd)?.shiftDays(deltaDays, zoneId)
            TimelineEdge.END -> effectiveStart
        }
        val updatedEnd = when (edge) {
            TimelineEdge.START -> effectiveEnd
            TimelineEdge.END -> (effectiveEnd ?: effectiveStart)?.shiftDays(deltaDays, zoneId)
        }

        return normalized.copy(
            startDate = updatedStart,
            dueDate = updatedEnd
        ).canonicalized()
    }

    fun scheduleBounds(task: Task, zoneId: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalDate>? {
        val normalized = task.canonicalized()
        val start = normalized.startDate?.toLocalDate(zoneId)
        val end = normalized.dueDate?.toLocalDate(zoneId)
        val effectiveStart = start ?: end
        val effectiveEnd = end ?: start
        if (effectiveStart == null || effectiveEnd == null) {
            return null
        }
        return effectiveStart to effectiveEnd
    }

    private fun buildRange(
        items: List<TimelineItem>,
        zoom: TimelineZoom,
        anchorDate: LocalDate,
        today: LocalDate
    ): TimelineRange {
        val padding = when (zoom) {
            TimelineZoom.DAY -> 5L
            TimelineZoom.WEEK -> 10L
            TimelineZoom.MONTH -> 20L
        }
        val minDate = (items.minOfOrNull { it.start } ?: anchorDate).coerceAtMost(today)
        val maxDate = (items.maxOfOrNull { it.end } ?: anchorDate).coerceAtLeast(today)
        val start = minDate.minusDays(padding)
        val end = maxDate.plusDays(padding)
        return TimelineRange(start = start, end = end)
    }
}

fun Task.canonicalizedTimeline(): Task {
    val normalizedTask = when {
        type == TaskType.MILESTONE -> {
            val milestoneDate = dueDate ?: startDate
            copy(startDate = milestoneDate, dueDate = milestoneDate)
        }
        startDate != null && dueDate != null && dueDate.before(startDate) -> {
            copy(dueDate = startDate)
        }
        else -> this
    }
    return normalizedTask
}

fun Task.canonicalized(): Task = canonicalizedTimeline().canonicalizedStatus()

private fun Date.toLocalDate(zoneId: ZoneId): LocalDate =
    toInstant().atZone(zoneId).toLocalDate()

private fun LocalDate.toDate(zoneId: ZoneId): Date =
    Date.from(atStartOfDay(zoneId).toInstant())

private fun Date.shiftDays(days: Long, zoneId: ZoneId): Date =
    toLocalDate(zoneId).plusDays(days).toDate(zoneId)
