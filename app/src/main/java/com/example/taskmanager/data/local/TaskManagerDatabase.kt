package com.example.taskmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.taskmanager.data.local.dao.ProjectDao
import com.example.taskmanager.data.local.dao.ProjectInvitationDao
import com.example.taskmanager.data.local.dao.ProjectMemberDao
import com.example.taskmanager.data.local.dao.TaskDao
import com.example.taskmanager.data.local.dao.UserDao
import com.example.taskmanager.data.local.entity.ProjectEntity
import com.example.taskmanager.data.local.entity.ProjectInvitationEntity
import com.example.taskmanager.data.local.entity.ProjectMemberEntity
import com.example.taskmanager.data.local.entity.TaskEntity
import com.example.taskmanager.data.local.entity.UserEntity
import com.example.taskmanager.data.local.util.Converters

// data/local/TaskManagerDatabase.kt
@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        UserEntity::class,
        ProjectMemberEntity::class,
        ProjectInvitationEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class TaskManagerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun userDao(): UserDao
    abstract fun projectMemberDao(): ProjectMemberDao
    abstract fun projectInvitationDao(): ProjectInvitationDao
}
