package com.saokt.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.saokt.taskmanager.data.local.entity.TaskEntity
import com.saokt.taskmanager.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("SELECT * FROM tasks WHERE syncStatus = :status")
    suspend fun getTasksBySyncStatus(status: SyncStatus): List<TaskEntity>


    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: String): Flow<TaskEntity?>

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("SELECT id FROM tasks")
    suspend fun getAllLocalTaskIds(): List<String>

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    suspend fun deleteByIds(taskIds: List<String>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("UPDATE tasks SET completed = :isCompleted, modifiedAt = :modifiedAt, syncStatus = :syncStatus WHERE id = :taskId")
    suspend fun updateTaskCompletion(
        taskId: String,
        isCompleted: Boolean,
        modifiedAt: Date,
        syncStatus: SyncStatus
    ): Int

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: String, status: SyncStatus)

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // Top-N most important uncompleted tasks: prioritize higher priority and earlier due dates.
    // Room SQL: order by completed first (false first), then priority (HIGH > MEDIUM > LOW),
    // then dueDate ascending with NULLS LAST so undated tasks come after dated ones.
    @Query(
        """
        SELECT * FROM tasks
        WHERE completed = 0
        ORDER BY
            CASE priority
                WHEN 'HIGH' THEN 3
                WHEN 'MEDIUM' THEN 2
                WHEN 'LOW' THEN 1
                ELSE 0
            END DESC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun getTopImportantUncompleted(limit: Int = 5): List<TaskEntity>
}
