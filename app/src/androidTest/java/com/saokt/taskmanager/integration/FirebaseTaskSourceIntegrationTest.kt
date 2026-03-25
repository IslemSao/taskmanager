package com.saokt.taskmanager.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.data.remote.firebase.FirebaseTaskSource
import com.saokt.taskmanager.di.AppModule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseTaskSourceIntegrationTest {
    private lateinit var context: Context
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var taskSource: FirebaseTaskSource
    private var emulatorEnabled = false

    @Before
    fun setUp() = runBlocking {
        emulatorEnabled = InstrumentationRegistry.getArguments()
            .getString("useFirebaseEmulator")
            ?.toBooleanStrictOrNull() == true
        assumeTrue(emulatorEnabled)

        context = ApplicationProvider.getApplicationContext()
        auth = AppModule.provideFirebaseAuth(context)
        firestore = AppModule.provideFirebaseFirestore(context)
        taskSource = FirebaseTaskSource(firestore, auth)
        auth.signOut()
    }

    @After
    fun tearDown() = runBlocking {
        if (::auth.isInitialized) {
            auth.signOut()
        }
    }

    @Test
    fun createTask_withoutProject_normalizesUserFieldsAndPersistsInEmulator() = runBlocking {
        val ownerEmail = uniqueEmail()
        val ownerPassword = "secret123"
        val ownerId = signUpAndReturnUid(ownerEmail, ownerPassword)
        val taskId = "task-${UUID.randomUUID()}"

        val result = taskSource.createTask(
            TaskDto(
                id = taskId,
                title = "Create integration task",
                description = "Task created against local emulator",
                createdBy = ownerId,
                userId = "",
                visibleToUserIds = emptyList(),
                createdAt = Date(),
                modifiedAt = Date()
            )
        )

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals(ownerId, created?.createdBy)
        assertEquals(ownerId, created?.userId)
        assertTrue(created?.visibleToUserIds?.contains(ownerId) == true)

        val stored = firestore.collection("tasks").document(taskId).get().await()
        assertTrue(stored.exists())
        assertEquals(ownerId, stored.getString("createdBy"))
        assertEquals(ownerId, stored.getString("userId"))
    }

    @Test
    fun createTask_projectScopedByMember_includesOwnerInVisibleUsers() = runBlocking {
        val ownerEmail = uniqueEmail()
        val ownerPassword = "secret123"
        val ownerId = signUpAndReturnUid(ownerEmail, ownerPassword)

        auth.signOut()
        val memberEmail = uniqueEmail()
        val memberPassword = "secret123"
        val memberId = signUpAndReturnUid(memberEmail, memberPassword)
        auth.signOut()

        signIn(ownerEmail, ownerPassword)
        val projectId = "project-${UUID.randomUUID()}"
        createProjectWithMember(projectId, ownerId, memberId)
        auth.signOut()

        signIn(memberEmail, memberPassword)
        val taskId = "task-${UUID.randomUUID()}"
        val result = taskSource.createTask(
            TaskDto(
                id = taskId,
                title = "Project member task",
                projectId = projectId,
                createdBy = memberId,
                userId = "",
                assignedTo = ownerId,
                createdAt = Date(),
                modifiedAt = Date()
            )
        )

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertTrue(created?.visibleToUserIds?.contains(memberId) == true)
        assertTrue(created?.visibleToUserIds?.contains(ownerId) == true)

        val stored = firestore.collection("tasks").document(taskId).get().await()
        assertTrue(stored.exists())
        val visibleTo = stored.get("visibleToUserIds") as? List<*>
        assertTrue(visibleTo?.contains(memberId) == true)
        assertTrue(visibleTo?.contains(ownerId) == true)
    }

    @Test
    fun createTask_projectScopedByNonMember_failsWithAuthorizationError() = runBlocking {
        val ownerEmail = uniqueEmail()
        val ownerPassword = "secret123"
        val ownerId = signUpAndReturnUid(ownerEmail, ownerPassword)

        val projectId = "project-${UUID.randomUUID()}"
        createProjectWithMember(projectId, ownerId, memberId = ownerId)
        auth.signOut()

        val outsiderEmail = uniqueEmail()
        val outsiderPassword = "secret123"
        val outsiderId = signUpAndReturnUid(outsiderEmail, outsiderPassword)

        val taskId = "task-${UUID.randomUUID()}"
        val result = taskSource.createTask(
            TaskDto(
                id = taskId,
                title = "Unauthorized project task",
                projectId = projectId,
                createdBy = outsiderId,
                createdAt = Date(),
                modifiedAt = Date()
            )
        )

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        val looksLikeAuthorizationFailure =
            message.contains("authorized", ignoreCase = true) ||
                message.contains("permission", ignoreCase = true)
        assertTrue(looksLikeAuthorizationFailure)
    }

    private suspend fun signUpAndReturnUid(email: String, password: String): String {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
        requireNotNull(user) { "Expected created user" }
        return user.uid
    }

    private suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    private suspend fun createProjectWithMember(projectId: String, ownerId: String, memberId: String) {
        val now = Date()
        firestore.collection("projects").document(projectId).set(
            mapOf(
                "id" to projectId,
                "title" to "Integration Project",
                "description" to "Task source integration project",
                "createdAt" to now,
                "modifiedAt" to now,
                "completed" to false,
                "ownerId" to ownerId,
                "members" to listOf(ownerId, memberId).distinct(),
                "memberIds" to listOf(ownerId, memberId).distinct()
            )
        ).await()
    }

    private fun uniqueEmail(): String = "task-int-${UUID.randomUUID()}@example.com"
}
