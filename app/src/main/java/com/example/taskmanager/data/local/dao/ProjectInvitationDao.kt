// data/local/dao/ProjectInvitationDao.kt
package com.example.taskmanager.data.local.dao

import androidx.room.*
import com.example.taskmanager.data.local.entity.ProjectInvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectInvitationDao {
    @Query("SELECT * FROM project_invitations")
    fun getAllInvitations(): Flow<List<ProjectInvitationEntity>>

    @Query("SELECT * FROM project_invitations WHERE id = :invitationId")
    fun getInvitationById(invitationId: String): Flow<ProjectInvitationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: ProjectInvitationEntity)

    @Update
    suspend fun updateInvitation(invitation: ProjectInvitationEntity)

    @Upsert // Handles both INSERT and UPDATE based on PrimaryKey (id)
    suspend fun upsertAllInvitations(invitations: List<ProjectInvitationEntity>)

    // Get only the IDs for efficient comparison
    @Query("SELECT id FROM project_invitations WHERE inviteeId = :userId") // Filter by user if invitations table stores for multiple users
    suspend fun getAllLocalInvitationIdsForUser(userId: String): List<String>

    // Delete invitations based on a list of IDs
    @Query("DELETE FROM project_invitations WHERE id IN (:ids)")
    suspend fun deleteInvitationsByIds(ids: List<String>): Int // Returns number of rows affected

    @Query("DELETE FROM project_invitations")
    suspend fun deleteAllInvitations()

    //getAllLocalInvitationIds
    @Query("SELECT id FROM project_invitations")
    suspend fun getAllLocalInvitationIds(): List<String>
}
