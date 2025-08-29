package com.saokt.taskmanager.data.remote.firebase

import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto

class FirebaseTaskSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val tasksCollection = firestore.collection("tasks")

    suspend fun createTask(taskDto: TaskDto): Result<TaskDto> {
        val debugTag = "TaskCreationDebug"
        Log.d(debugTag, "FirebaseTaskSource: createTask called with taskDto: $taskDto")
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            Log.d(debugTag, "createTask called. currentUserId: $currentUserId, input taskDto: $taskDto")

            // Check permissions for task creation
            val canCreate = when {
                taskDto.projectId != null -> {
                    // Check if user is project owner or member
                    val projectDoc = firestore.collection("projects").document(taskDto.projectId).get().await()
                    val project = projectDoc.toObject(com.saokt.taskmanager.data.remote.dto.ProjectDto::class.java)
                    project?.let { 
                        it.ownerId == currentUserId || currentUserId in it.members 
                    } ?: false
                }
                else -> {
                    // For tasks without projects, user can create their own tasks
                    taskDto.createdBy == currentUserId
                }
            }

            if (!canCreate) {
                return Result.failure(IllegalAccessException("Not authorized to create this task"))
            }

            // Determine the userId for the task:
            // - If task is assigned to someone, use the assigned user's ID
            // - If task is not assigned, use the current user's ID
            val taskUserId = taskDto.assignedTo ?: currentUserId
            Log.d(debugTag, "Determined taskUserId: $taskUserId (assignedTo: ${taskDto.assignedTo})")

            val taskWithUser = taskDto.copy(
                userId = taskUserId,
                createdBy = currentUserId
            )
            Log.d(debugTag, "Final taskWithUser to be saved: $taskWithUser")
            tasksCollection.document(taskWithUser.id).set(taskWithUser).await()
            Log.d(debugTag, "Task successfully created in Firestore with id: ${taskWithUser.id}")
            Result.success(taskWithUser)
        } catch (e: Exception) {
            Log.e(debugTag, "Error creating task", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(taskDto: TaskDto): Result<TaskDto> {
        val debugTag = "TaskCreationDebug"
        Log.d(debugTag, "FirebaseTaskSource: updateTask called with taskDto: $taskDto")
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Allow updates if user is the creator, assigned user, or project owner
            val canUpdate = taskDto.createdBy == userId || 
                           taskDto.assignedTo == userId ||
                           taskDto.userId == userId

            if (!canUpdate) {
                return Result.failure(IllegalAccessException("Not authorized to update this task"))
            }

            tasksCollection.document(taskDto.id).set(taskDto).await()
            Result.success(taskDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId : String): Result<Unit> {
        return try {
            tasksCollection.document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTasks(): Result<List<TaskDto>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Get tasks where userId matches the current user
            val tasksSnapshot = tasksCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val tasks = tasksSnapshot.documents.mapNotNull { 
                it.toObject(TaskDto::class.java)?.copy(id = it.id)
            }
            
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTasksByProject(projectId: String): Result<List<TaskDto>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            Log.d("ProjectTasksDebug", "FirebaseTaskSource: getTasksByProject called for projectId: $projectId, userId: $userId")

            // First, check if user is project owner
            val projectDoc = firestore.collection("projects").document(projectId).get().await()
            val project = projectDoc.toObject(com.saokt.taskmanager.data.remote.dto.ProjectDto::class.java)
            
            val isProjectOwner = project?.ownerId == userId
            Log.d("ProjectTasksDebug", "FirebaseTaskSource: Is project owner: $isProjectOwner")

            val snapshot = tasksCollection
                .whereEqualTo("projectId", projectId)
                .get()
                .await()

            val allTasks = snapshot.documents.mapNotNull { 
                it.toObject(TaskDto::class.java)?.copy(id = it.id)
            }
            
            Log.d("ProjectTasksDebug", "FirebaseTaskSource: All tasks for project: ${allTasks.size}")
            allTasks.forEach { task ->
                Log.d("ProjectTasksDebug", "FirebaseTaskSource: Task: ${task.title}, userId: ${task.userId}, createdBy: ${task.createdBy}, assignedTo: ${task.assignedTo}")
            }

            val filteredTasks = if (isProjectOwner) {
                // If user is project owner, return all tasks
                Log.d("ProjectTasksDebug", "FirebaseTaskSource: User is project owner - returning all tasks")
                allTasks
            } else {
                // If user is not project owner, only return their own tasks
                Log.d("ProjectTasksDebug", "FirebaseTaskSource: User is not project owner - filtering tasks")
                allTasks.filter { task ->
                    val isOwnedByUser = task.userId == userId
                    val isAssignedToUser = task.assignedTo == userId
                    val isCreatedByUser = task.createdBy == userId
                    Log.d("ProjectTasksDebug", "FirebaseTaskSource: Task: ${task.title}, isOwnedByUser: $isOwnedByUser, isAssignedToUser: $isAssignedToUser, isCreatedByUser: $isCreatedByUser")
                    isOwnedByUser || isAssignedToUser || isCreatedByUser
                }
            }
            
            Log.d("ProjectTasksDebug", "FirebaseTaskSource: Final filtered tasks count: ${filteredTasks.size}")
            Result.success(filteredTasks)
        } catch (e: Exception) {
            Log.e("ProjectTasksDebug", "FirebaseTaskSource: Error getting tasks by project", e)
            Result.failure(e)
        }
    }

    suspend fun assignTask(taskId: String, assignedToUserId: String, assignedByUserId: String): Result<TaskDto> {
        return try {
            val taskDoc = tasksCollection.document(taskId).get().await()
            val task = taskDoc.toObject(TaskDto::class.java)
                ?: return Result.failure(IllegalStateException("Task not found"))

            // Check if the user has permission to assign this task
            // Project owners can assign tasks to any project member
            // Task creators can assign their own tasks
            // Assigned users can reassign their tasks
            val canAssign = when {
                task.projectId != null -> {
                    // Check if user is project owner
                    val projectDoc = firestore.collection("projects").document(task.projectId).get().await()
                    val project = projectDoc.toObject(com.saokt.taskmanager.data.remote.dto.ProjectDto::class.java)
                    project?.ownerId == assignedByUserId
                }
                else -> {
                    // For tasks without projects, only creator or assigned user can assign
                    task.createdBy == assignedByUserId || 
                    task.assignedTo == assignedByUserId ||
                    task.userId == assignedByUserId
                }
            }

            if (!canAssign) {
                return Result.failure(IllegalAccessException("Not authorized to assign this task"))
            }

            val updatedTask = task.copy(
                assignedTo = assignedToUserId,
                assignedBy = assignedByUserId,
                assignedAt = java.util.Date()
            )

            tasksCollection.document(taskId).set(updatedTask).await()
            Result.success(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectMembers(projectId: String): Result<List<ProjectMemberDto>> {
        val debugTag = "TaskCreationDebug"
        Log.d(debugTag, "FirebaseTaskSource: getProjectMembers called for projectId: $projectId")
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Assuming ProjectMemberDto has projectId and userId fields
            val snapshot = firestore.collection("projectMembers")
                .whereEqualTo("projectId", projectId)
                .get()
                .await()

            val members = snapshot.documents.mapNotNull {
                it.toObject(ProjectMemberDto::class.java)?.copy(projectId = it.id)
            }
            Result.success(members)
        } catch (e: Exception) {
            Log.e(debugTag, "Error getting project members", e)
            Result.failure(e)
        }
    }
}
