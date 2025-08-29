package com.saokt.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.saokt.taskmanager.data.local.entity.ProjectEntity
import com.saokt.taskmanager.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY dueDate")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    // --- For UI Observation ---
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>> // UI observes this

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?>

    // --- For Syncing ---
    @Upsert // Handles both INSERT and UPDATE based on PrimaryKey (id)
    suspend fun upsertAll(projects: List<ProjectEntity>)

    // Get all IDs currently in the local DB (for calculating deletions)
    // NOTE: If your DB might store data for multiple users and isn't cleared on logout,
    // you might need a WHERE clause here based on ownerId or add a userId column.
    // Assuming for now it's cleared or only for the current user. Adjust if needed.
    @Query("SELECT id FROM projects")
    suspend fun getAllLocalProjectIds(): List<String>

    // Delete projects by their IDs
    @Query("DELETE FROM projects WHERE id IN (:projectIds)")
    suspend fun deleteByIds(projectIds: List<String>)

    // --- Other methods you might need ---
    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT * FROM projects WHERE syncStatus = :status")
    suspend fun getProjectsBySyncStatus(status: SyncStatus): List<ProjectEntity>

    @Query("UPDATE projects SET syncStatus = :status WHERE id = :projectId")
    suspend fun updateSyncStatus(projectId: String, status: SyncStatus)
}
