package com.saokt.taskmanager.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.saokt.taskmanager.data.remote.dto.ChatMessageDto
import com.saokt.taskmanager.data.remote.dto.ChatThreadDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseChatSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val threadsCol = firestore.collection("chat_threads")

    fun listenThreadsByProject(projectId: String): Flow<List<ChatThreadDto>> = callbackFlow {
        val reg = threadsCol
            .whereEqualTo("projectId", projectId)
            .orderBy("lastUpdatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseChat", "listenThreads error", e)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChatThreadDto::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }

    fun listenMessages(threadId: String): Flow<List<ChatMessageDto>> = callbackFlow {
        Log.d("FirebaseChat", "listenMessages start: threadId=$threadId")
        val reg = threadsCol.document(threadId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseChat", "listenMessages error", e)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChatMessageDto::class.java)?.copy(id = it.id) } ?: emptyList()
                Log.d("FirebaseChat", "listenMessages update: count=${list.size}")
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }

    suspend fun createOrGetThread(projectId: String, taskId: String?, participants: List<String>): Result<ChatThreadDto> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
            val finalParticipants = (participants + userId).distinct()
            Log.d("FirebaseChat", "createOrGetThread: projectId=$projectId taskId=$taskId participants=${finalParticipants.joinToString()}")

            // Query for existing thread with same participants + project + task
            val query = threadsCol
                .whereEqualTo("projectId", projectId)
                .whereArrayContains("participantIds", userId)
                .get().await()

            val found = query.documents.mapNotNull { it.toObject(ChatThreadDto::class.java)?.copy(id = it.id) }
                .firstOrNull { it.taskId == taskId && it.participantIds.toSet() == finalParticipants.toSet() }
            if (found != null) return Result.success(found)

            val doc = threadsCol.document()
            val dto = ChatThreadDto(
                id = doc.id,
                projectId = projectId,
                taskId = taskId,
                participantIds = finalParticipants,
                createdAt = java.util.Date(),
                lastMessagePreview = null,
                lastUpdatedAt = java.util.Date()
            )
            doc.set(dto).await()
            Result.success(dto)
        } catch (e: Exception) {
            Log.e("FirebaseChat", "createOrGetThread error", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(threadId: String, message: ChatMessageDto): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
            if (message.senderId != userId) return Result.failure(IllegalAccessException("Sender mismatch"))

            val msgRef = threadsCol.document(threadId).collection("messages").document(message.id)
            msgRef.set(message).await()

            // update thread preview
            threadsCol.document(threadId).update(
                mapOf(
                    "lastMessagePreview" to message.text.take(120),
                    "lastUpdatedAt" to message.timestamp
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseChat", "sendMessage error", e)
            Result.failure(e)
        }
    }

    suspend fun markAsRead(threadId: String, userId: String): Result<Unit> {
        return try {
            val user = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
            if (user != userId) return Result.failure(IllegalAccessException("User mismatch"))
            // mark latest message read-by
            val msgs = threadsCol.document(threadId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            val batch = firestore.batch()
            msgs.documents.forEach { doc ->
                val readBy = (doc.get("readBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (!readBy.contains(userId)) {
                    batch.update(doc.reference, "readBy", readBy + userId)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseChat", "markAsRead error", e)
            Result.failure(e)
        }
    }
}
