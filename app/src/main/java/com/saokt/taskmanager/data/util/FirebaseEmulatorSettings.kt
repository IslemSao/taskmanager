package com.saokt.taskmanager.data.util

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirebaseEmulatorSettings {
    private const val ENABLED_KEY = "taskmanager.firebase.emulator.enabled"
    private const val HOST_KEY = "taskmanager.firebase.emulator.host"
    private const val AUTH_PORT_KEY = "taskmanager.firebase.auth.emulator.port"
    private const val FIRESTORE_PORT_KEY = "taskmanager.firebase.firestore.emulator.port"
    private const val PROJECT_ID_KEY = "taskmanager.firebase.emulator.projectId"

    private const val DEFAULT_HOST = "10.0.2.2"
    private const val DEFAULT_AUTH_PORT = 9099
    private const val DEFAULT_FIRESTORE_PORT = 8080
    private const val DEFAULT_PROJECT_ID = "demo-taskmanager"
    private val configuredAppNames = mutableSetOf<String>()

    fun isEnabled(): Boolean = System.getProperty(ENABLED_KEY)?.toBooleanStrictOrNull() == true

    fun emulatorProjectId(): String = System.getProperty(PROJECT_ID_KEY)?.takeIf { it.isNotBlank() }
        ?: DEFAULT_PROJECT_ID

    fun configureIfNeeded(firebaseApp: FirebaseApp, auth: FirebaseAuth, firestore: FirebaseFirestore) {
        if (!isEnabled()) return
        synchronized(configuredAppNames) {
            if (!configuredAppNames.add(firebaseApp.name)) return
        }

        val host = System.getProperty(HOST_KEY)?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
        val authPort = System.getProperty(AUTH_PORT_KEY)?.toIntOrNull() ?: DEFAULT_AUTH_PORT
        val firestorePort = System.getProperty(FIRESTORE_PORT_KEY)?.toIntOrNull() ?: DEFAULT_FIRESTORE_PORT

        auth.useEmulator(host, authPort)
        firestore.useEmulator(host, firestorePort)
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }
}
