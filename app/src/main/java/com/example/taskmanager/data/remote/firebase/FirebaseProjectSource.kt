package com.example.taskmanager.data.remote.firebase

import android.util.Log
import com.example.taskmanager.data.remote.dto.ProjectDto
import com.example.taskmanager.data.remote.dto.ProjectInvitationDto
import com.example.taskmanager.data.remote.dto.ProjectMemberDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseProjectSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val projectsCollection = firestore.collection("projects")
    private val invitationsCollection = firestore.collection("project_invitations")
private val tasksRef = firestore.collection("tasks")
    private val invitationsRef = firestore.collection("project_invitations")
    private val membersRef = firestore.collection("project_members")
    suspend fun createProject(projectDto: ProjectDto): Result<ProjectDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val projectWithUser = projectDto.copy(id = userId)
            projectsCollection.document(projectWithUser.id).set(projectWithUser).await()
            Result.success(projectWithUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // In your Firebase implementation class
    suspend fun deleteAllTasksForProject(projectId: String) {
        val query = tasksRef.whereEqualTo("projectId", projectId)
        val snapshot = query.get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    suspend fun deleteAllInvitationsForProject(projectId: String) {
        val query = invitationsRef.whereEqualTo("projectId", projectId)
        val snapshot = query.get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    suspend fun deleteAllMembersForProject(projectId: String) {
        val query = membersRef.whereEqualTo("projectId", projectId)
        val snapshot = query.get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            projectsCollection.document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(projectDto: ProjectDto): Result<ProjectDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            if (projectDto.ownerId != userId) {
                return Result.failure(IllegalAccessException("Not authorized to update this project"))
            }

            projectsCollection.document(projectDto.id).set(projectDto).await()
            Result.success(projectDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllProjects(): Result<List<ProjectDto>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val snapshot = projectsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val projects = snapshot.documents.mapNotNull { it.toObject(ProjectDto::class.java) }
            Result.success(projects)
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    suspend fun createInvitation(invitationDto: ProjectInvitationDto): Result<ProjectInvitationDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Verify the inviter is the current user
            if (invitationDto.inviterId != userId) {
                return Result.failure(IllegalAccessException("Not authorized to send this invitation"))
            }

            // Create the invitation in Firestore
            invitationsCollection.document(invitationDto.id).set(invitationDto).await()
            Result.success(invitationDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun addProjectMember(projectId: String, member: ProjectMemberDto): Result<Unit> {
        return try {
            Log.d("sahoor", "Adding member to project: $member")
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Verify the project exists
            val project = projectsCollection.document(projectId).get().await()
                .toObject(ProjectDto::class.java)
                ?: return Result.failure(IllegalStateException("Project not found"))

            // Create a reference to the members subcollection within the project
            val membersSubcollection = projectsCollection.document(projectId).collection("members")

            // Add the member to the subcollection, using member's userId as document ID
            membersSubcollection.document(member.userId)
                .set(member)
                .await()

            // Still update the members array in the project document
            val updatedMembers = project.members.toMutableList().apply {
                add(member.userId)
            }

            projectsCollection.document(projectId)
                .update("members", updatedMembers)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("sahoor", "Error adding project member", e)
            Result.failure(e)
        }
    }


    suspend fun updateInvitation(invitationDto: ProjectInvitationDto): Result<ProjectInvitationDto> {
        return try {
            // Update the invitation in Firestore
            invitationsCollection.document(invitationDto.id).set(invitationDto).await()
            Result.success(invitationDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvitations(userId: String): Result<List<ProjectInvitationDto>> {
        return try {
            // Get invitations where the user is the invitee
            val snapshot = invitationsCollection
                .whereEqualTo("inviteeId", userId)
                .get()
                .await()

            val invitations =
                snapshot.documents.mapNotNull { it.toObject(ProjectInvitationDto::class.java) }
            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
