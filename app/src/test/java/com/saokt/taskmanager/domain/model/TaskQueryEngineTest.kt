package com.saokt.taskmanager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

class TaskQueryEngineTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-23T09:00:00Z"), ZoneOffset.UTC)

    private val taskA = Task(
        id = "a",
        title = "Alpha",
        dueDate = Date.from(Instant.parse("2026-03-23T10:00:00Z")),
        priority = Priority.HIGH,
        assignedTo = "u1",
        createdBy = "u2",
        modifiedAt = Date.from(Instant.parse("2026-03-22T10:00:00Z"))
    )
    private val taskB = Task(
        id = "b",
        title = "Beta",
        dueDate = Date.from(Instant.parse("2026-03-25T10:00:00Z")),
        priority = Priority.MEDIUM,
        status = TaskStatus.IN_PROGRESS,
        assignedTo = "u2",
        createdBy = "u1",
        modifiedAt = Date.from(Instant.parse("2026-03-24T10:00:00Z"))
    )
    private val taskC = Task(
        id = "c",
        title = "Gamma",
        dueDate = null,
        completed = true,
        status = TaskStatus.DONE,
        priority = Priority.LOW,
        createdBy = "u3",
        modifiedAt = Date.from(Instant.parse("2026-03-21T10:00:00Z"))
    )
    private val taskD = Task(
        id = "d",
        title = "Delta",
        dueDate = Date.from(Instant.parse("2026-03-20T10:00:00Z")),
        priority = Priority.HIGH,
        projectId = "p2",
        createdBy = "u4",
        modifiedAt = Date.from(Instant.parse("2026-03-20T10:00:00Z"))
    )

    @Test
    fun `filters by combined criteria`() {
        val query = TaskListQuery(
            status = TaskStatusFilter.OPEN,
            assignment = TaskAssignmentFilter.ASSIGNED_TO_ME,
            priorities = setOf(Priority.HIGH),
            dueDate = DueDateBucket.TODAY
        )

        val result = TaskQueryEngine.apply(
            tasks = listOf(taskA, taskB, taskC, taskD),
            query = query,
            currentUserId = "u1",
            clock = clock
        )

        assertThat(result.map { it.id }).containsExactly("a")
    }

    @Test
    fun `sorts with null due dates last for ascending due date`() {
        val result = TaskQueryEngine.apply(
            tasks = listOf(taskC, taskB, taskA),
            query = TaskListQuery(sort = TaskSort.DUE_DATE_ASC),
            currentUserId = "u1",
            clock = clock
        )

        assertThat(result.map { it.id }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `supports overdue and no due date buckets`() {
        val overdue = TaskQueryEngine.apply(
            tasks = listOf(taskA, taskB, taskC, taskD),
            query = TaskListQuery(dueDate = DueDateBucket.OVERDUE),
            currentUserId = null,
            clock = clock
        )
        val noDueDate = TaskQueryEngine.apply(
            tasks = listOf(taskA, taskB, taskC, taskD),
            query = TaskListQuery(dueDate = DueDateBucket.NO_DUE_DATE),
            currentUserId = null,
            clock = clock
        )

        assertThat(overdue.map { it.id }).containsExactly("d")
        assertThat(noDueDate.map { it.id }).containsExactly("c")
    }

    @Test
    fun `groups tasks by workflow status`() {
        val grouped = TaskQueryEngine.groupByStatus(listOf(taskA, taskB, taskC))

        assertThat(grouped[TaskStatus.TODO]?.map { it.id }).containsExactly("a")
        assertThat(grouped[TaskStatus.IN_PROGRESS]?.map { it.id }).containsExactly("b")
        assertThat(grouped[TaskStatus.DONE]?.map { it.id }).containsExactly("c")
    }
}
