package com.saokt.taskmanager.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
    
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Log.d("TaskWidget", "Widget enabled")
    }
    
    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d("TaskWidget", "Widget disabled")
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("TaskWidget", "Widget update requested for ${appWidgetIds.size} widgets")
    }
}
