package com.saokt.taskmanager.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Refreshes all instances of the top tasks widget. Safe to call from background threads. */
    suspend fun refreshTopTasksWidget() {
        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(TaskWidget::class.java)
            val widget = TaskWidget()
            ids.forEach { id ->
                widget.update(context, id)
            }
        } catch (t: Throwable) {
            Log.e("WidgetRefresher", "Failed to update TaskWidget", t)
        }
    }
}
