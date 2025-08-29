// data/local/dao/ProjectMemberDao.kt
package com.saokt.taskmanager.data.local.dao

import androidx.room.*
import com.saokt.taskmanager.data.local.entity.ProjectMemberEntity
import com.saokt.taskmanager.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMemberDao {
    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    fun getMembersByProject(projectId: String): Flow<List<ProjectMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ProjectMemberEntity)

    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun deleteMember(projectId: String, userId: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId")
    suspend fun deleteMembersByProject(projectId: String)

    @Query("SELECT * FROM project_members WHERE projectId IN (:projectIds)")
    suspend fun getMembersForProjects(projectIds: List<String>): List<ProjectMemberEntity>

    @Delete
    suspend fun deleteMembers(members: List<ProjectMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<ProjectMemberEntity>)

    @Query("SELECT * FROM project_members ")
    fun getAllMembers(): Flow<List<ProjectMemberEntity>>

    @Query("DELETE FROM project_members ")
    suspend fun deleteAllMembers()

    //getMembersForProjectsList
    @Query("SELECT * FROM project_members WHERE projectId IN (:projectIds)")
    suspend fun getMembersForProjectsList(projectIds: List<String>): List<ProjectMemberEntity>
}

