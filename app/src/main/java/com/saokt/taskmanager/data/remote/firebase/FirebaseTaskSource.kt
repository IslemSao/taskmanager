package com.saokt.taskmanager.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirebaseTaskSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FirebaseTaskSource"
        private const val PROJECTS_COLLECTION = "projects"
        private const val TASKS_COLLECTION = "tasks"
        private const val MEMBERS_SUBCOLLECTION = "members"
    }

    private val tasksCollection = firestore.collection(TASKS_COLLECTION)

    suspend fun getTaskById(taskId: String): Result<TaskDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val doc = tasksCollection.document(taskId).get().await()
            val dto = doc.toDto<TaskDto>()?.copy(id = doc.id)
                ?: return Result.failure(IllegalStateException("Task not found"))
            val canView = dto.visibleToUserIds.contains(userId) ||
                dto.createdBy == userId ||
                dto.assignedTo == userId ||
                dto.userId == userId
            if (!canView) {
                return Result.failure(IllegalAccessException("Not authorized to view this task"))
            }
            Result.success(dto)
        } catch (e: Exception) {
            Log.e(TAG, "getTaskById failed", e)
            Result.failure(e)
        }
    }

    suspend fun createTask(taskDto: TaskDto): Result<TaskDto> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            if (taskDto.createdBy != currentUserId) {
                return Result.failure(IllegalAccessException("Creator mismatch"))
            }

            val visibleToUserIds = computeVisibleUsers(
                projectId = taskDto.projectId,
                creatorId = currentUserId,
                assignedTo = taskDto.assignedTo
            )
            val normalizedTask = taskDto.copy(
                userId = currentUserId,
                createdBy = currentUserId,
                visibleToUserIds = visibleToUserIds
            )

            ensureCanAccessProject(normalizedTask.projectId, currentUserId)
            tasksCollection.document(normalizedTask.id).set(normalizedTask).await()
            Result.success(normalizedTask)
        } catch (e: Exception) {
            Log.e(TAG, "createTask failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(taskDto: TaskDto): Result<TaskDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val canUpdate = taskDto.visibleToUserIds.contains(userId) ||
                taskDto.createdBy == userId ||
                isProjectOwner(taskDto.projectId, userId)
            if (!canUpdate) {
                return Result.failure(IllegalAccessException("Not authorized to update this task"))
            }

            val visibleToUserIds = computeVisibleUsers(
                projectId = taskDto.projectId,
                creatorId = taskDto.createdBy.ifBlank { userId },
                assignedTo = taskDto.assignedTo
            )
            val normalizedTask = taskDto.copy(
                createdBy = taskDto.createdBy.ifBlank { userId },
                userId = taskDto.userId.ifBlank { taskDto.createdBy.ifBlank { userId } },
                visibleToUserIds = visibleToUserIds,
                modifiedAt = Date()
            )

            ensureCanAccessProject(normalizedTask.projectId, userId)
            tasksCollection.document(normalizedTask.id).set(normalizedTask).await()
            Result.success(normalizedTask)
        } catch (e: Exception) {
            Log.e(TAG, "updateTask failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val task = tasksCollection.document(taskId).get().await().toDto<TaskDto>()?.copy(id = taskId)
                ?: return Result.failure(IllegalStateException("Task not found"))
            val canDelete = task.visibleToUserIds.contains(userId) ||
                task.createdBy == userId ||
                isProjectOwner(task.projectId, userId)
            if (!canDelete) {
                return Result.failure(IllegalAccessException("Not authorized to delete this task"))
            }
            tasksCollection.document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTask failed", e)
            Result.failure(e)
        }
    }

    suspend fun getAllTasks(): Result<List<TaskDto>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val tasks = tasksCollection
                .whereArrayContains("visibleToUserIds", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toDto<TaskDto>()?.copy(id = it.id) }
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "getAllTasks failed", e)
            Result.failure(e)
        }
    }

    suspend fun getTasksByProject(projectId: String): Result<List<TaskDto>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val tasks = tasksCollection
                .whereEqualTo("projectId", projectId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toDto<TaskDto>()?.copy(id = it.id) }
                .filter { task ->
                    task.visibleToUserIds.contains(userId) ||
                        task.createdBy == userId ||
                        task.assignedTo == userId ||
                        task.userId == userId
                }
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByProject failed", e)
            Result.failure(e)
        }
    }

    suspend fun assignTask(taskId: String, assignedToUserId: String, assignedByUserId: String): Result<TaskDto> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            if (currentUserId != assignedByUserId) {
                return Result.failure(IllegalAccessException("User mismatch"))
            }

            val task = tasksCollection.document(taskId).get().await().toDto<TaskDto>()?.copy(id = taskId)
                ?: return Result.failure(IllegalStateException("Task not found"))
            val canAssign = task.visibleToUserIds.contains(assignedByUserId) ||
                task.createdBy == assignedByUserId ||
                isProjectOwner(task.projectId, assignedByUserId)
            if (!canAssign) {
                return Result.failure(IllegalAccessException("Not authorized to assign this task"))
            }

            val updatedTask = task.copy(
                assignedTo = assignedToUserId,
                assignedBy = assignedByUserId,
                assignedAt = Date(),
                modifiedAt = Date(),
                visibleToUserIds = computeVisibleUsers(task.projectId, task.createdBy, assignedToUserId)
            )

            tasksCollection.document(taskId).set(updatedTask).await()
            Result.success(updatedTask)
        } catch (e: Exception) {
            Log.e(TAG, "assignTask failed", e)
            Result.failure(e)
        }
    }

    suspend fun getProjectMembers(projectId: String): Result<List<ProjectMemberDto>> {
        return try {
            val snapshot = firestore.collection(PROJECTS_COLLECTION)
                .document(projectId)
                .collection(MEMBERS_SUBCOLLECTION)
                .get()
                .await()
            val members = snapshot.documents.mapNotNull { doc ->
                doc.toDto<ProjectMemberDto>()?.copy(projectId = projectId, userId = doc.id)
            }
            Result.success(members)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectMembers failed", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureCanAccessProject(projectId: String?, userId: String) {
        if (projectId == null) {
            return
        }
        val project = firestore.collection(PROJECTS_COLLECTION).document(projectId).get().await()
            .toDto<ProjectDto>()
            ?: throw IllegalStateException("Project not found")
        val canAccess = project.ownerId == userId || project.members.contains(userId) || project.memberIds.contains(userId)
        if (!canAccess) {
            throw IllegalAccessException("Not authorized to access this project")
        }
    }

    private suspend fun computeVisibleUsers(
        projectId: String?,
        creatorId: String,
        assignedTo: String?
    ): List<String> {
        val visibleUsers = linkedSetOf(creatorId)
        if (!assignedTo.isNullOrBlank()) {
            visibleUsers += assignedTo
        }
        if (projectId != null) {
            val project = firestore.collection(PROJECTS_COLLECTION).document(projectId).get().await()
                .toDto<ProjectDto>()
            if (project != null) {
                if (project.ownerId.isNotBlank()) {
                    visibleUsers += project.ownerId
                }
            }
        }
        return visibleUsers.toList()
    }

    private suspend fun isProjectOwner(projectId: String?, userId: String): Boolean {
        if (projectId == null) {
            return false
        }
        val project = firestore.collection(PROJECTS_COLLECTION).document(projectId).get().await()
            .toDto<ProjectDto>()
        return project?.ownerId == userId
    }

    private inline fun <reified T> DocumentSnapshot.toDto(): T? = toObject(T::class.java)
}
