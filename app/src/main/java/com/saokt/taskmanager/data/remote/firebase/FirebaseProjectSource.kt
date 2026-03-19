package com.saokt.taskmanager.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseProjectSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FirebaseProjectSource"
        private const val PROJECTS_COLLECTION = "projects"
        private const val INVITATIONS_COLLECTION = "project_invitations"
        private const val TASKS_COLLECTION = "tasks"
        private const val LEGACY_MEMBERS_COLLECTION = "project_members"
        private const val LEGACY_MEMBERS_COLLECTION_CAMEL = "projectMembers"
        private const val MEMBERS_SUBCOLLECTION = "members"
    }

    private val projectsCollection = firestore.collection(PROJECTS_COLLECTION)
    private val invitationsCollection = firestore.collection(INVITATIONS_COLLECTION)
    private val tasksCollection = firestore.collection(TASKS_COLLECTION)

    suspend fun createProject(projectDto: ProjectDto): Result<ProjectDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            if (projectDto.ownerId != userId) {
                return Result.failure(IllegalAccessException("Not authorized to create this project"))
            }

            val normalizedMembers = projectDto.members.ifEmpty { listOf(userId) }.distinct()
            val normalizedProject = projectDto.copy(
                members = normalizedMembers,
                memberIds = normalizedMembers
            )
            projectsCollection.document(normalizedProject.id).set(normalizedProject).await()
            Result.success(normalizedProject)
        } catch (e: Exception) {
            Log.e(TAG, "createProject failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAllTasksForProject(projectId: String) {
        val snapshot = tasksCollection.whereEqualTo("projectId", projectId).get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun deleteAllInvitationsForProject(projectId: String) {
        val snapshot = invitationsCollection.whereEqualTo("projectId", projectId).get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun deleteAllMembersForProject(projectId: String) {
        val membersSnapshot = projectsCollection.document(projectId)
            .collection(MEMBERS_SUBCOLLECTION)
            .get()
            .await()
        val batch = firestore.batch()
        membersSnapshot.documents.forEach { batch.delete(it.reference) }

        val legacySnapshots = listOf(
            firestore.collection(LEGACY_MEMBERS_COLLECTION).whereEqualTo("projectId", projectId).get().await(),
            firestore.collection(LEGACY_MEMBERS_COLLECTION_CAMEL).whereEqualTo("projectId", projectId).get().await()
        )
        legacySnapshots.forEach { snapshot ->
            snapshot.documents.forEach { batch.delete(it.reference) }
        }
        batch.commit().await()
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            projectsCollection.document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteProject failed", e)
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

            val normalizedMembers = projectDto.members.ifEmpty { listOf(userId) }.distinct()
            val normalizedProject = projectDto.copy(
                members = normalizedMembers,
                memberIds = normalizedMembers
            )
            projectsCollection.document(normalizedProject.id)
                .set(normalizedProject, SetOptions.merge())
                .await()
            Result.success(normalizedProject)
        } catch (e: Exception) {
            Log.e(TAG, "updateProject failed", e)
            Result.failure(e)
        }
    }

    suspend fun getAllProjects(): Result<List<ProjectDto>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val ownedSnapshot = projectsCollection.whereEqualTo("ownerId", userId).get().await()
            val memberSnapshot = projectsCollection.whereArrayContains("members", userId).get().await()
            val projects = (ownedSnapshot.documents + memberSnapshot.documents)
                .distinctBy { it.id }
                .mapNotNull { doc -> doc.toDto<ProjectDto>()?.copy(id = doc.id) }
            Result.success(projects)
        } catch (e: Exception) {
            Log.e(TAG, "getAllProjects failed", e)
            Result.failure(e)
        }
    }

    suspend fun getProjectById(projectId: String): Result<ProjectDto?> {
        return try {
            val project = projectsCollection.document(projectId).get().await()
                .toDto<ProjectDto>()
                ?.copy(id = projectId)
            Result.success(project)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectById failed", e)
            Result.failure(e)
        }
    }

    suspend fun getProjectMembers(projectId: String): Result<List<ProjectMemberDto>> {
        return try {
            migrateLegacyMembersIfNeeded(projectId)

            val snapshot = projectsCollection.document(projectId)
                .collection(MEMBERS_SUBCOLLECTION)
                .get()
                .await()

            val members = snapshot.documents.mapNotNull { doc ->
                doc.toDto<ProjectMemberDto>()?.copy(
                    projectId = projectId,
                    userId = doc.id
                )
            }
            Result.success(members.sortedBy { it.displayName.lowercase() })
        } catch (e: Exception) {
            Log.e(TAG, "getProjectMembers failed for $projectId", e)
            Result.failure(e)
        }
    }

    suspend fun createInvitation(invitationDto: ProjectInvitationDto): Result<ProjectInvitationDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            if (invitationDto.inviterId != userId) {
                return Result.failure(IllegalAccessException("Not authorized to send this invitation"))
            }

            invitationsCollection.document(invitationDto.id).set(invitationDto).await()
            Result.success(invitationDto)
        } catch (e: Exception) {
            Log.e(TAG, "createInvitation failed", e)
            Result.failure(e)
        }
    }

    suspend fun addProjectMember(projectId: String, member: ProjectMemberDto): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val projectSnapshot = projectsCollection.document(projectId).get().await()
            val project = projectSnapshot.toDto<ProjectDto>()
                ?: return Result.failure(IllegalStateException("Project not found"))

            if (project.ownerId != currentUserId && member.userId != currentUserId) {
                return Result.failure(IllegalAccessException("Not authorized to add this member"))
            }

            projectsCollection.document(projectId)
                .collection(MEMBERS_SUBCOLLECTION)
                .document(member.userId)
                .set(member.copy(projectId = projectId))
                .await()

            projectsCollection.document(projectId)
                .update(
                    mapOf(
                        "members" to FieldValue.arrayUnion(member.userId),
                        "memberIds" to FieldValue.arrayUnion(member.userId)
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addProjectMember failed", e)
            Result.failure(e)
        }
    }

    suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val project = projectsCollection.document(projectId).get().await().toDto<ProjectDto>()
                ?: return Result.failure(IllegalStateException("Project not found"))
            if (project.ownerId != currentUserId) {
                return Result.failure(IllegalAccessException("Only owners can remove members"))
            }

            val batch = firestore.batch()
            batch.delete(
                projectsCollection.document(projectId)
                    .collection(MEMBERS_SUBCOLLECTION)
                    .document(userId)
            )
            batch.update(
                projectsCollection.document(projectId),
                mapOf(
                    "members" to FieldValue.arrayRemove(userId),
                    "memberIds" to FieldValue.arrayRemove(userId)
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "removeProjectMember failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateInvitation(invitationDto: ProjectInvitationDto): Result<ProjectInvitationDto> {
        return try {
            invitationsCollection.document(invitationDto.id).set(invitationDto, SetOptions.merge()).await()
            Result.success(invitationDto)
        } catch (e: Exception) {
            Log.e(TAG, "updateInvitation failed", e)
            Result.failure(e)
        }
    }

    suspend fun getInvitations(userId: String): Result<List<ProjectInvitationDto>> {
        return try {
            val email = auth.currentUser?.email
            val queries = buildList {
                add(invitationsCollection.whereEqualTo("inviteeId", userId).get().await())
                if (!email.isNullOrBlank()) {
                    add(invitationsCollection.whereEqualTo("inviteeEmail", email).get().await())
                }
            }
            val invitations = queries
                .flatMap { it.documents }
                .distinctBy { it.id }
                .mapNotNull { it.toDto<ProjectInvitationDto>()?.copy(id = it.id) }
            Result.success(invitations)
        } catch (e: Exception) {
            Log.e(TAG, "getInvitations failed", e)
            Result.failure(e)
        }
    }

    private suspend fun migrateLegacyMembersIfNeeded(projectId: String) {
        val subcollection = projectsCollection.document(projectId).collection(MEMBERS_SUBCOLLECTION)
        if (!subcollection.get().await().isEmpty) {
            return
        }

        val legacyDocs = (
            firestore.collection(LEGACY_MEMBERS_COLLECTION).whereEqualTo("projectId", projectId).get().await().documents +
                firestore.collection(LEGACY_MEMBERS_COLLECTION_CAMEL).whereEqualTo("projectId", projectId).get().await().documents
            )
            .distinctBy { "${it.getString("projectId")}:${it.getString("userId") ?: it.id}" }

        if (legacyDocs.isEmpty()) {
            return
        }

        val batch = firestore.batch()
        val memberIds = mutableListOf<String>()
        legacyDocs.forEach { doc ->
            val mappedMember = doc.toDto<ProjectMemberDto>()?.copy(
                projectId = projectId,
                userId = doc.getString("userId") ?: doc.id
            ) ?: return@forEach
            memberIds += mappedMember.userId
            batch.set(subcollection.document(mappedMember.userId), mappedMember)
        }
        if (memberIds.isNotEmpty()) {
            batch.update(
                projectsCollection.document(projectId),
                mapOf(
                    "members" to memberIds.distinct(),
                    "memberIds" to memberIds.distinct()
                )
            )
        }
        batch.commit().await()
    }

    private inline fun <reified T> DocumentSnapshot.toDto(): T? = toObject(T::class.java)
}
