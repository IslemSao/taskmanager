package com.saokt.taskmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.saokt.taskmanager.data.local.dao.ProjectDao
import com.saokt.taskmanager.data.local.dao.ProjectInvitationDao
import com.saokt.taskmanager.data.local.dao.ProjectMemberDao
import com.saokt.taskmanager.data.local.dao.TaskDao
import com.saokt.taskmanager.data.local.dao.UserDao
import com.saokt.taskmanager.data.local.entity.ProjectEntity
import com.saokt.taskmanager.data.local.entity.ProjectInvitationEntity
import com.saokt.taskmanager.data.local.entity.ProjectMemberEntity
import com.saokt.taskmanager.data.local.entity.TaskEntity
import com.saokt.taskmanager.data.local.entity.UserEntity
import com.saokt.taskmanager.data.local.util.Converters

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
