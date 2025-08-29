package com.saokt.taskmanager.di

import android.content.Context
import androidx.room.Room
import com.saokt.taskmanager.data.local.TaskManagerDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTaskManagerDatabase(@ApplicationContext context: Context): TaskManagerDatabase {
        return Room.databaseBuilder(
            context,
            TaskManagerDatabase::class.java,
            "task_manager_db"
        ).build()
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
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore


    @Provides
    @Singleton
    fun provideProjectInvitationDao(database: TaskManagerDatabase) = database.projectInvitationDao()

    @Provides
    @Singleton
    fun provideProjectMemberDao(database: TaskManagerDatabase) = database.projectMemberDao()

}
