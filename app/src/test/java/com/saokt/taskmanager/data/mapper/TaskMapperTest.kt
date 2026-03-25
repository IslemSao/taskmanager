package com.saokt.taskmanager.data.mapper

import com.google.common.truth.Truth.assertThat
import com.saokt.taskmanager.data.local.entity.TaskEntity
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.Priority
import com.saokt.taskmanager.domain.model.SyncStatus
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.TaskType
import org.junit.Test
import java.util.Date

class TaskMapperTest {
    private val mapper = TaskMapper()

    @Test
    fun `dto without status falls back to completed flag`() {
        val dto = TaskDto(
            id = "task-1",
            title = "Legacy task",
            completed = true,
            status = null,
            createdAt = Date(),
            modifiedAt = Date(),
            userId = "u1",
            createdBy = "u1"
        )

        val task = mapper.dtoToDomain(dto)

        assertThat(task.status).isEqualTo(TaskStatus.DONE)
        assertThat(task.completed).isTrue()
    }

    @Test
    fun `entity maps explicit workflow status`() {
        val entity = TaskEntity(
            id = "task-2",
            title = "Board task",
            description = "",
            startDate = Date(1_700_000_000_000L),
            dueDate = null,
            completed = false,
            status = TaskStatus.IN_PROGRESS,
            type = TaskType.TASK,
            priority = Priority.MEDIUM,
            projectId = "p1",
            labels = emptyList(),
            subtasks = emptyList(),
            createdAt = Date(),
            modifiedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            userId = "u1",
            createdBy = "u1",
            assignedTo = null,
            assignedBy = null,
            visibleToUserIds = emptyList()
        )

        val task = mapper.entityToDomain(entity)
        val dto = mapper.domainToDto(task)

        assertThat(task.status).isEqualTo(TaskStatus.IN_PROGRESS)
        assertThat(task.completed).isFalse()
        assertThat(dto.status).isEqualTo("IN_PROGRESS")
        assertThat(task.startDate).isEqualTo(entity.startDate)
    }

    @Test
    fun `milestone dto maps task type and canonical date`() {
        val date = Date(1_700_100_000_000L)
        val dto = TaskDto(
            id = "milestone-1",
            title = "Launch",
            startDate = null,
            dueDate = date,
            type = "MILESTONE",
            createdAt = Date(),
            modifiedAt = Date(),
            userId = "u1",
            createdBy = "u1"
        )

        val task = mapper.dtoToDomain(dto)
        val entity = mapper.domainToEntity(task)

        assertThat(task.type).isEqualTo(TaskType.MILESTONE)
        assertThat(task.startDate).isEqualTo(date)
        assertThat(task.dueDate).isEqualTo(date)
        assertThat(entity.type).isEqualTo(TaskType.MILESTONE)
    }
}
