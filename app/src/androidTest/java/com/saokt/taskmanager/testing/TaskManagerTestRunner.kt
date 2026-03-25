package com.saokt.taskmanager.testing

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class TaskManagerTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        val useFirebaseEmulator = arguments.getString("useFirebaseEmulator")?.toBooleanStrictOrNull() == true
        if (useFirebaseEmulator) {
            System.setProperty("taskmanager.firebase.emulator.enabled", "true")
            System.setProperty(
                "taskmanager.firebase.emulator.host",
                arguments.getString("firebaseEmulatorHost") ?: "10.0.2.2"
            )
            System.setProperty(
                "taskmanager.firebase.auth.emulator.port",
                arguments.getString("firebaseAuthEmulatorPort") ?: "9099"
            )
            System.setProperty(
                "taskmanager.firebase.firestore.emulator.port",
                arguments.getString("firebaseFirestoreEmulatorPort") ?: "8080"
            )
            System.setProperty(
                "taskmanager.firebase.emulator.projectId",
                arguments.getString("firebaseEmulatorProjectId") ?: "demo-taskmanager"
            )
        }
        super.onCreate(arguments)
    }
}
