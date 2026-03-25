package com.saokt.taskmanager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

class TaskTimelineEngineTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-25T09:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `build separates scheduled and unscheduled tasks`() {
        val scheduled = Task(
            id = "scheduled",
            title = "Scheduled",
            startDate = Date.from(Instant.parse("2026-03-24T00:00:00Z")),
            dueDate = Date.from(Instant.parse("2026-03-27T00:00:00Z"))
        )
        val unscheduled = Task(id = "unscheduled", title = "Unscheduled")

        val timeline = TaskTimelineEngine.build(
            tasks = listOf(scheduled, unscheduled),
            zoom = TimelineZoom.WEEK,
            anchorDate = null,
            canEditSchedule = { true },
            clock = clock
        )

        assertThat(timeline.items.map { it.task.id }).containsExactly("scheduled")
        assertThat(timeline.unscheduledTasks.map { it.id }).containsExactly("unscheduled")
    }

    @Test
    fun `milestone canonicalization uses same start and due date`() {
        val milestoneDate = Date.from(Instant.parse("2026-03-30T00:00:00Z"))
        val milestone = Task(
            id = "m1",
            title = "Launch",
            dueDate = milestoneDate,
            type = TaskType.MILESTONE
        )

        val normalized = milestone.canonicalized()

        assertThat(normalized.startDate).isEqualTo(milestoneDate)
        assertThat(normalized.dueDate).isEqualTo(milestoneDate)
    }

    @Test
    fun `shiftTask schedules undated task on current day`() {
        val task = Task(id = "new", title = "Plan me")

        val shifted = TaskTimelineEngine.shiftTask(task, 0, ZoneOffset.UTC)

        assertThat(shifted.startDate).isNotNull()
        assertThat(shifted.dueDate).isNotNull()
    }

    @Test
    fun `resizeTask keeps due date after start date`() {
        val task = Task(
            id = "resize",
            title = "Resize me",
            startDate = Date.from(Instant.parse("2026-03-26T00:00:00Z")),
            dueDate = Date.from(Instant.parse("2026-03-27T00:00:00Z"))
        )

        val resized = TaskTimelineEngine.resizeTask(task, TimelineEdge.END, -3, ZoneOffset.UTC)

        assertThat(resized.startDate).isEqualTo(resized.dueDate)
    }
}
