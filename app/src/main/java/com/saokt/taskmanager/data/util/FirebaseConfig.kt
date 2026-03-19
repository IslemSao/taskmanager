package com.saokt.taskmanager.data.util

import android.content.Context

object FirebaseConfig {
    private const val FALLBACK_APP_ID = "1:1234567890:android:replace-me"
    private const val FALLBACK_API_KEY = "replace-me"
    private const val FALLBACK_WEB_CLIENT_ID = "replace-me.apps.googleusercontent.com"

    fun isConfigured(context: Context): Boolean {
        val appId = getStringResource(context, "google_app_id")
        val apiKey = getStringResource(context, "google_api_key")

        return !appId.isNullOrBlank() &&
            !apiKey.isNullOrBlank() &&
            appId != FALLBACK_APP_ID &&
            apiKey != FALLBACK_API_KEY
    }

    fun getGoogleWebClientId(context: Context): String? {
        val webClientId = getStringResource(context, "default_web_client_id")
        return webClientId?.takeIf {
            it.isNotBlank() &&
                it != FALLBACK_WEB_CLIENT_ID &&
                !it.contains("replace-me", ignoreCase = true)
        }
    }

    private fun getStringResource(context: Context, name: String): String? {
        val resourceId = context.resources.getIdentifier(name, "string", context.packageName)
        if (resourceId == 0) {
            return null
        }
        return context.getString(resourceId).takeIf { it.isNotBlank() }
    }
}
