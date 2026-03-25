package com.saokt.taskmanager.domain.repository

import com.saokt.taskmanager.domain.model.ProjectTaskViewMode
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.TaskListViewMode
import com.saokt.taskmanager.domain.model.TimelineZoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface TaskPreferencesRepository {
    fun observeTaskListQuery(): Flow<TaskListQuery>
    suspend fun saveTaskListQuery(query: TaskListQuery)

    fun observeTaskListViewMode(): Flow<TaskListViewMode> = flowOf(TaskListViewMode.LIST)
    suspend fun saveTaskListViewMode(mode: TaskListViewMode) {}

    fun observeTaskListTimelineZoom(): Flow<TimelineZoom> = flowOf(TimelineZoom.WEEK)
    suspend fun saveTaskListTimelineZoom(zoom: TimelineZoom) {}

    fun observeTaskListTimelineAnchor(): Flow<Long?> = flowOf(null)
    suspend fun saveTaskListTimelineAnchor(anchorEpochDay: Long?) {}

    fun observeProjectTaskQuery(): Flow<TaskListQuery>
    suspend fun saveProjectTaskQuery(query: TaskListQuery)

    fun observeProjectTaskViewMode(): Flow<ProjectTaskViewMode>
    suspend fun saveProjectTaskViewMode(mode: ProjectTaskViewMode)

    fun observeProjectTaskTimelineZoom(): Flow<TimelineZoom> = flowOf(TimelineZoom.WEEK)
    suspend fun saveProjectTaskTimelineZoom(zoom: TimelineZoom) {}

    fun observeProjectTaskTimelineAnchor(): Flow<Long?> = flowOf(null)
    suspend fun saveProjectTaskTimelineAnchor(anchorEpochDay: Long?) {}
}
