package com.saokt.taskmanager.di

import android.content.Context
import android.util.Log
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saokt.taskmanager.data.local.TaskManagerDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saokt.taskmanager.data.util.FirebaseEmulatorSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val TAG = "AppModule"
    private const val FALLBACK_APP_ID = "1:1234567890:android:replace-me"
    private const val FALLBACK_API_KEY = "replace-me"
    private const val FALLBACK_PROJECT_ID = "local-taskmanager"
    private const val FALLBACK_SENDER_ID = "1234567890"
    private const val FALLBACK_STORAGE_BUCKET = "local-taskmanager.appspot.com"

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN visibleToUserIds TEXT NOT NULL DEFAULT '[]'"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN status TEXT NOT NULL DEFAULT 'TODO'"
            )
            database.execSQL(
                "UPDATE tasks SET status = CASE WHEN completed = 1 THEN 'DONE' ELSE 'TODO' END"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN startDate INTEGER"
            )
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN type TEXT NOT NULL DEFAULT 'TASK'"
            )
        }
    }

    @Provides
    @Singleton
    fun provideTaskManagerDatabase(@ApplicationContext context: Context): TaskManagerDatabase {
        return Room.databaseBuilder(
            context,
            TaskManagerDatabase::class.java,
            "task_manager_db"
        )
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: TaskManagerDatabase) = database.taskDao()

    @Provides
    @Singleton
    fun provideProjectDao(database: TaskManagerDatabase) = database.projectDao()

    @Provides
    @Singleton
    fun provideUserDao(database: TaskManagerDatabase) = database.userDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth {
        val firebaseApp = ensureFirebaseApp(context)
        return FirebaseAuth.getInstance(firebaseApp).also { auth ->
            FirebaseEmulatorSettings.configureIfNeeded(
                firebaseApp = firebaseApp,
                auth = auth,
                firestore = FirebaseFirestore.getInstance(firebaseApp)
            )
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore {
        val firebaseApp = ensureFirebaseApp(context)
        return FirebaseFirestore.getInstance(firebaseApp).also { firestore ->
            FirebaseEmulatorSettings.configureIfNeeded(
                firebaseApp = firebaseApp,
                auth = FirebaseAuth.getInstance(firebaseApp),
                firestore = firestore
            )
        }
    }


    @Provides
    @Singleton
    fun provideProjectInvitationDao(database: TaskManagerDatabase) = database.projectInvitationDao()

    @Provides
    @Singleton
    fun provideProjectMemberDao(database: TaskManagerDatabase) = database.projectMemberDao()

    private fun ensureFirebaseApp(context: Context): FirebaseApp {
        FirebaseApp.getApps(context).firstOrNull()?.let { return it }
        FirebaseApp.initializeApp(context)?.let { return it }

        Log.w(TAG, "Firebase config not found. Initializing a fallback FirebaseApp so the app can start in local-only mode.")

        val options = FirebaseOptions.Builder()
            .setApplicationId(getStringResource(context, "google_app_id") ?: FALLBACK_APP_ID)
            .setApiKey(getStringResource(context, "google_api_key") ?: FALLBACK_API_KEY)
            .setProjectId(
                getStringResource(context, "project_id")
                    ?: if (FirebaseEmulatorSettings.isEnabled()) {
                        FirebaseEmulatorSettings.emulatorProjectId()
                    } else {
                        FALLBACK_PROJECT_ID
                    }
            )
            .setGcmSenderId(getStringResource(context, "gcm_defaultSenderId") ?: FALLBACK_SENDER_ID)
            .setStorageBucket(getStringResource(context, "google_storage_bucket") ?: FALLBACK_STORAGE_BUCKET)
            .build()

        return FirebaseApp.initializeApp(context, options)
            ?: FirebaseApp.getApps(context).first()
    }

    private fun getStringResource(context: Context, name: String): String? {
        val resourceId = context.resources.getIdentifier(name, "string", context.packageName)
        if (resourceId == 0) {
            return null
        }
        return context.getString(resourceId).takeIf { it.isNotBlank() }
    }
}
