package com.example.taskmanager.data.remote.firebase

import com.example.taskmanager.data.remote.dto.TaskDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseTaskSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val tasksCollection = firestore.collection("tasks")

    suspend fun createTask(taskDto: TaskDto): Result<TaskDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val taskWithUser = taskDto.copy(userId = userId)
            tasksCollection.document(taskWithUser.id).set(taskWithUser).await()
            Result.success(taskWithUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(taskDto: TaskDto): Result<TaskDto> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Ensure the task belongs to the current user
            if (taskDto.userId != userId) {
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

            val snapshot = tasksCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val tasks = snapshot.documents.mapNotNull { it.toObject(TaskDto::class.java) }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
