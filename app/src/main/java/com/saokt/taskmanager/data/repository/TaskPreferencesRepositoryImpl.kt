package com.saokt.taskmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.saokt.taskmanager.domain.model.ProjectTaskViewMode
import com.saokt.taskmanager.domain.model.TaskListQuery
import com.saokt.taskmanager.domain.model.TaskListViewMode
import com.saokt.taskmanager.domain.model.TimelineZoom
import com.saokt.taskmanager.domain.repository.TaskPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.taskPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "task_preferences"
)

@Singleton
class TaskPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TaskPreferencesRepository {
    private val gson = Gson()

    override fun observeTaskListQuery(): Flow<TaskListQuery> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[TASK_LIST_QUERY_KEY]?.let(::decodeQuery) ?: TaskListQuery()
            }
    }

    override suspend fun saveTaskListQuery(query: TaskListQuery) {
        context.taskPreferencesDataStore.edit { preferences ->
            preferences[TASK_LIST_QUERY_KEY] = gson.toJson(query)
        }
    }

    override fun observeTaskListViewMode(): Flow<TaskListViewMode> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                preferences[TASK_LIST_VIEW_MODE_KEY]
                    ?.let(TaskListViewMode::valueOf)
                    ?: TaskListViewMode.LIST
            }
    }

    override suspend fun saveTaskListViewMode(mode: TaskListViewMode) {
        context.taskPreferencesDataStore.edit { preferences ->
            preferences[TASK_LIST_VIEW_MODE_KEY] = mode.name
        }
    }

    override fun observeTaskListTimelineZoom(): Flow<TimelineZoom> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                preferences[TASK_LIST_TIMELINE_ZOOM_KEY]
                    ?.let(TimelineZoom::valueOf)
                    ?: TimelineZoom.WEEK
            }
    }

    override suspend fun saveTaskListTimelineZoom(zoom: TimelineZoom) {
        context.taskPreferencesDataStore.edit { preferences ->
            preferences[TASK_LIST_TIMELINE_ZOOM_KEY] = zoom.name
        }
    }

    override fun observeTaskListTimelineAnchor(): Flow<Long?> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences -> preferences[TASK_LIST_TIMELINE_ANCHOR_KEY]?.toLongOrNull() }
    }

    override suspend fun saveTaskListTimelineAnchor(anchorEpochDay: Long?) {
        context.taskPreferencesDataStore.edit { preferences ->
            if (anchorEpochDay == null) {
                preferences.remove(TASK_LIST_TIMELINE_ANCHOR_KEY)
            } else {
                preferences[TASK_LIST_TIMELINE_ANCHOR_KEY] = anchorEpochDay.toString()
            }
        }
    }

    override fun observeProjectTaskQuery(): Flow<TaskListQuery> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PROJECT_TASK_QUERY_KEY]?.let(::decodeQuery) ?: TaskListQuery()
            }
    }

    override suspend fun saveProjectTaskQuery(query: TaskListQuery) {
        context.taskPreferencesDataStore.edit { preferences ->
            preferences[PROJECT_TASK_QUERY_KEY] = gson.toJson(query)
        }
    }

    override fun observeProjectTaskViewMode(): Flow<ProjectTaskViewMode> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PROJECT_TASK_VIEW_MODE_KEY]
                    ?.let(ProjectTaskViewMode::valueOf)
                    ?: ProjectTaskViewMode.LIST
            }
    }

    override suspend fun saveProjectTaskViewMode(mode: ProjectTaskViewMode) {
        context.taskPreferencesDataStore.edit { preferences ->
            preferences[PROJECT_TASK_VIEW_MODE_KEY] = mode.name
        }
    }

    override fun observeProjectTaskTimelineZoom(): Flow<TimelineZoom> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                preferences[PROJECT_TASK_TIMELINE_ZOOM_KEY]
                    ?.let(TimelineZoom::valueOf)
                    ?: TimelineZoom.WEEK
            }
    }

    override suspend fun saveProjectTaskTimelineZoom(zoom: TimelineZoom) {
        context.taskPreferencesDataStore.edit { preferences ->
            preferences[PROJECT_TASK_TIMELINE_ZOOM_KEY] = zoom.name
        }
    }

    override fun observeProjectTaskTimelineAnchor(): Flow<Long?> {
        return context.taskPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences -> preferences[PROJECT_TASK_TIMELINE_ANCHOR_KEY]?.toLongOrNull() }
    }

    override suspend fun saveProjectTaskTimelineAnchor(anchorEpochDay: Long?) {
        context.taskPreferencesDataStore.edit { preferences ->
            if (anchorEpochDay == null) {
                preferences.remove(PROJECT_TASK_TIMELINE_ANCHOR_KEY)
            } else {
                preferences[PROJECT_TASK_TIMELINE_ANCHOR_KEY] = anchorEpochDay.toString()
            }
        }
    }

    private fun decodeQuery(value: String): TaskListQuery {
        return runCatching { gson.fromJson(value, TaskListQuery::class.java) }
            .getOrDefault(TaskListQuery())
    }

    private companion object {
        val TASK_LIST_QUERY_KEY = stringPreferencesKey("task_list_query")
        val TASK_LIST_VIEW_MODE_KEY = stringPreferencesKey("task_list_view_mode")
        val TASK_LIST_TIMELINE_ZOOM_KEY = stringPreferencesKey("task_list_timeline_zoom")
        val TASK_LIST_TIMELINE_ANCHOR_KEY = stringPreferencesKey("task_list_timeline_anchor")
        val PROJECT_TASK_QUERY_KEY = stringPreferencesKey("project_task_query")
        val PROJECT_TASK_VIEW_MODE_KEY = stringPreferencesKey("project_task_view_mode")
        val PROJECT_TASK_TIMELINE_ZOOM_KEY = stringPreferencesKey("project_task_timeline_zoom")
        val PROJECT_TASK_TIMELINE_ANCHOR_KEY = stringPreferencesKey("project_task_timeline_anchor")
    }
}
