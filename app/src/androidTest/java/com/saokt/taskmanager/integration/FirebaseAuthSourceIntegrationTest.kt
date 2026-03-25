package com.saokt.taskmanager.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saokt.taskmanager.data.remote.firebase.FirebaseAuthSource
import com.saokt.taskmanager.di.AppModule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class FirebaseAuthSourceIntegrationTest {

    private lateinit var context: Context
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authSource: FirebaseAuthSource
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
        authSource = FirebaseAuthSource(auth, firestore)

        auth.signOut()
    }

    @After
    fun tearDown() = runBlocking {
        if (::auth.isInitialized) {
            auth.currentUser?.delete()?.await()
            auth.signOut()
        }
    }

    @Test
    fun signUp_createsRealFirebaseAuthUserAndUpdatesProfile() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"
        val displayName = "Jane Emulator"

        val result = authSource.signUp(email, password, displayName)

        assertTrue(result.isSuccess)
        val userDto = result.getOrNull()
        assertNotNull(userDto)
        assertEquals(email, userDto?.email)
        assertEquals(displayName, userDto?.displayName)
        assertEquals(email, auth.currentUser?.email)
        assertEquals(displayName, auth.currentUser?.displayName)
        val storedUser = firestore.collection("users").document(userDto!!.id).get().await()
        assertTrue(storedUser.exists())
        assertEquals(email, storedUser.getString("email"))
        assertEquals(displayName, storedUser.getString("displayName"))
    }

    @Test
    fun signUp_withDuplicateEmailFailsAgainstRealAuthEmulator() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"

        val first = authSource.signUp(email, password, "Jane One")
        auth.signOut()
        val second = authSource.signUp(email, password, "Jane Two")

        assertTrue(first.isSuccess)
        assertTrue(second.isFailure)
        assertTrue(second.exceptionOrNull()?.message.orEmpty().contains("already", ignoreCase = true))
    }

    @Test
    fun signIn_authenticatesExistingUserAgainstRealAuthEmulator() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"
        val displayName = "Jane Sign In"

        authSource.signUp(email, password, displayName)
        auth.signOut()

        val result = authSource.signIn(email, password)

        assertTrue(result.isSuccess)
        assertEquals(email, result.getOrNull()?.email)
        assertEquals(email, auth.currentUser?.email)
        assertEquals(displayName, auth.currentUser?.displayName)
        val storedUser = firestore.collection("users").document(auth.currentUser!!.uid).get().await()
        assertTrue(storedUser.exists())
        assertEquals(email, storedUser.getString("email"))
    }

    @Test
    fun signIn_withWrongPasswordFailsAgainstRealAuthEmulator() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"

        authSource.signUp(email, password, "Jane Wrong Password")
        auth.signOut()

        val result = authSource.signIn(email, "bad-password")

        assertTrue(result.isFailure)
        assertNull(auth.currentUser)
    }

    @Test
    fun signOut_clearsAuthenticatedUserAgainstRealAuthEmulator() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"

        authSource.signUp(email, password, "Jane Sign Out")
        assertNotNull(auth.currentUser)

        val result = authSource.signOut()

        assertTrue(result.isSuccess)
        assertNull(auth.currentUser)
    }

    @Test
    fun deleteAccount_removesAuthUserAndFirestoreUserDocument() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"
        val displayName = "Jane Delete"

        val signUpResult = authSource.signUp(email, password, displayName)
        assertTrue(signUpResult.isSuccess)
        val userId = signUpResult.getOrNull()!!.id
        assertTrue(firestore.collection("users").document(userId).get().await().exists())

        val deleteResult = authSource.deleteAccount()

        assertTrue(deleteResult.isSuccess)
        assertNull(auth.currentUser)

        val signInAfterDelete = authSource.signIn(email, password)
        assertTrue(signInAfterDelete.isFailure)
    }

    @Test
    fun deleteAccount_cleansMembershipTaskAssignmentAndInvitationsFromOtherUsersData() = runBlocking {
        val memberEmail = uniqueEmail()
        val memberPassword = "secret123"
        val memberDisplayName = "Member Delete"
        val memberResult = authSource.signUp(memberEmail, memberPassword, memberDisplayName)
        assertTrue(memberResult.isSuccess)
        val memberId = memberResult.getOrNull()!!.id
        auth.signOut()

        val ownerEmail = uniqueEmail()
        val ownerPassword = "secret123"
        val ownerDisplayName = "Owner User"
        val ownerResult = authSource.signUp(ownerEmail, ownerPassword, ownerDisplayName)
        assertTrue(ownerResult.isSuccess)
        val ownerId = ownerResult.getOrNull()!!.id
        assertNotEquals(memberId, ownerId)

        val projectId = "project-${UUID.randomUUID()}"
        val taskId = "task-${UUID.randomUUID()}"
        val invitationId = "invitation-${UUID.randomUUID()}"

        createProjectOwnedByCurrentUser(
            projectId = projectId,
            ownerId = ownerId,
            memberId = memberId,
            memberEmail = memberEmail,
            memberDisplayName = memberDisplayName
        )
        createTaskVisibleToOwnerAndMember(
            taskId = taskId,
            projectId = projectId,
            ownerId = ownerId,
            memberId = memberId
        )
        createInvitationForMember(
            invitationId = invitationId,
            projectId = projectId,
            projectTitle = "Cleanup Project",
            ownerId = ownerId,
            ownerDisplayName = ownerDisplayName,
            memberEmail = memberEmail
        )

        auth.signOut()
        val memberSignIn = authSource.signIn(memberEmail, memberPassword)
        assertTrue(memberSignIn.isSuccess)

        val deleteResult = authSource.deleteAccount()

        assertTrue(deleteResult.isSuccess)
        assertNull(auth.currentUser)
        assertTrue(authSource.signIn(memberEmail, memberPassword).isFailure)

        val ownerSignIn = authSource.signIn(ownerEmail, ownerPassword)
        assertTrue(ownerSignIn.isSuccess)

        val projectSnapshot = firestore.collection("projects").document(projectId).get().await()
        assertTrue(projectSnapshot.exists())
        assertFalse((projectSnapshot.get("members") as? List<*>)?.contains(memberId) == true)
        assertFalse((projectSnapshot.get("memberIds") as? List<*>)?.contains(memberId) == true)

        val memberDoc = firestore.collection("projects")
            .document(projectId)
            .collection("members")
            .document(memberId)
            .get()
            .await()
        assertFalse(memberDoc.exists())

        val taskSnapshot = firestore.collection("tasks").document(taskId).get().await()
        assertTrue(taskSnapshot.exists())
        assertNull(taskSnapshot.getString("assignedTo"))
        assertNull(taskSnapshot.get("assignedAt"))
        assertNull(taskSnapshot.getString("assignedBy"))
        assertFalse((taskSnapshot.get("visibleToUserIds") as? List<*>)?.contains(memberId) == true)

        val remainingInvitations = firestore.collection("project_invitations")
            .whereEqualTo("inviterId", ownerId)
            .whereEqualTo("projectId", projectId)
            .get()
            .await()
        assertTrue(remainingInvitations.documents.none { it.id == invitationId })
    }

    @Test
    fun deleteAccount_withoutSignedInUserFails() = runBlocking {
        auth.signOut()

        val result = authSource.deleteAccount()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("No user signed in"))
    }

    @Test
    fun signIn_recreatesMissingUserDocumentForExistingAuthUser() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"
        val displayName = "Recreate User Doc"

        val signUpResult = authSource.signUp(email, password, displayName)
        assertTrue(signUpResult.isSuccess)
        val userId = signUpResult.getOrNull()!!.id

        firestore.collection("users").document(userId).delete().await()
        auth.signOut()

        val signInResult = authSource.signIn(email, password)

        assertTrue(signInResult.isSuccess)
        val restoredUser = firestore.collection("users").document(userId).get().await()
        assertTrue(restoredUser.exists())
        assertEquals(email, restoredUser.getString("email"))
        assertEquals(displayName, restoredUser.getString("displayName"))
    }

    @Test
    fun signInWithGoogle_acceptsMockJwtAgainstAuthEmulator() = runBlocking {
        val result = authSource.signInWithGoogle(
            """{"sub":"google-${UUID.randomUUID()}","email":"google-${UUID.randomUUID()}@example.com","email_verified":true,"name":"Google Emulator"}"""
        )

        assertTrue(result.isSuccess)
        assertEquals("Google Emulator", result.getOrNull()?.displayName)
        assertTrue(auth.currentUser?.isEmailVerified == true)
        val storedUser = firestore.collection("users").document(result.getOrNull()!!.id).get().await()
        assertTrue(storedUser.exists())
        assertEquals(result.getOrNull()!!.email, storedUser.getString("email"))
    }

    @Test
    fun sendPasswordResetEmail_generatesPasswordResetOobCode() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"
        authSource.signUp(email, password, "Reset User")
        auth.signOut()
        val result = authSource.sendPasswordResetEmail(email)

        assertTrue(result.isSuccess)
        val passwordResetEntry = getLatestOobCodeEntry("PASSWORD_RESET", email)
        assertNotNull(passwordResetEntry)
    }

    @Test
    fun sendEmailVerification_generatesOobCodeAndMarksUserVerifiedAfterActionLink() = runBlocking {
        val email = uniqueEmail()
        val password = "secret123"
        val signUpResult = authSource.signUp(email, password, "Verify User")
        assertTrue(signUpResult.isSuccess)
        val sendResult = authSource.sendEmailVerification()

        assertTrue(sendResult.isSuccess)
        val verificationEntry = getLatestOobCodeEntry("VERIFY_EMAIL", email)
        assertNotNull(verificationEntry)

        val actionUrl = verificationEntry!!.getString("oobLink")
            .replace("localhost", "10.0.2.2")
            .replace("127.0.0.1", "10.0.2.2")
        val actionResponse = httpGet(actionUrl)
        assertTrue(actionResponse.contains("success", ignoreCase = true))

        val verificationStatus = authSource.isCurrentUserEmailVerified(forceRefresh = true)
        assertTrue(verificationStatus.isSuccess)
        assertTrue(verificationStatus.getOrNull() == true)
    }

    private fun uniqueEmail(): String = "codex-${UUID.randomUUID()}@example.com"

    private fun authEmulatorBaseUrl(): String {
        val host = InstrumentationRegistry.getArguments().getString("firebaseEmulatorHost") ?: "10.0.2.2"
        val port = InstrumentationRegistry.getArguments().getString("firebaseAuthEmulatorPort") ?: "9099"
        return "http://$host:$port"
    }

    private fun getLatestOobCodeEntry(requestType: String, email: String): JSONObject? {
        val response = httpGet("${authEmulatorBaseUrl()}/emulator/v1/projects/${firebaseEmulatorProjectId()}/oobCodes")
        val codes = JSONObject(response).optJSONArray("oobCodes") ?: JSONArray()
        for (index in 0 until codes.length()) {
            val item = codes.getJSONObject(index)
            if (item.optString("requestType") == requestType && item.optString("email") == email) {
                return item
            }
        }
        return null
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        return connection.inputStream.bufferedReader().use { it.readText() }.also {
            connection.disconnect()
        }
    }

    private fun firebaseEmulatorProjectId(): String {
        return InstrumentationRegistry.getArguments().getString("firebaseEmulatorProjectId") ?: "demo-taskmanager"
    }

    private suspend fun createProjectOwnedByCurrentUser(
        projectId: String,
        ownerId: String,
        memberId: String,
        memberEmail: String,
        memberDisplayName: String
    ) {
        val now = Date()
        firestore.collection("projects").document(projectId).set(
            mapOf(
                "id" to projectId,
                "title" to "Cleanup Project",
                "description" to "Project used for delete-account cleanup coverage",
                "createdAt" to now,
                "modifiedAt" to now,
                "completed" to false,
                "ownerId" to ownerId,
                "members" to listOf(ownerId, memberId),
                "memberIds" to listOf(ownerId, memberId)
            )
        ).await()

        firestore.collection("projects")
            .document(projectId)
            .collection("members")
            .document(ownerId)
            .set(
                mapOf(
                    "projectId" to projectId,
                    "userId" to ownerId,
                    "email" to ownerEmail(),
                    "displayName" to "Owner User",
                    "role" to "OWNER",
                    "joinedAt" to now
                )
            ).await()

        firestore.collection("projects")
            .document(projectId)
            .collection("members")
            .document(memberId)
            .set(
                mapOf(
                    "projectId" to projectId,
                    "userId" to memberId,
                    "email" to memberEmail,
                    "displayName" to memberDisplayName,
                    "role" to "MEMBER",
                    "joinedAt" to now
                )
            ).await()
    }

    private suspend fun createTaskVisibleToOwnerAndMember(
        taskId: String,
        projectId: String,
        ownerId: String,
        memberId: String
    ) {
        val now = Date()
        firestore.collection("tasks").document(taskId).set(
            mapOf(
                "id" to taskId,
                "title" to "Assigned cleanup task",
                "description" to "Verifies task cleanup on account deletion",
                "completed" to false,
                "priority" to "MEDIUM",
                "projectId" to projectId,
                "labels" to emptyList<String>(),
                "subtasks" to emptyList<Map<String, Any>>(),
                "createdAt" to now,
                "modifiedAt" to now,
                "userId" to ownerId,
                "createdBy" to ownerId,
                "assignedTo" to memberId,
                "assignedAt" to now,
                "assignedBy" to ownerId,
                "visibleToUserIds" to listOf(ownerId, memberId)
            )
        ).await()
    }

    private suspend fun createInvitationForMember(
        invitationId: String,
        projectId: String,
        projectTitle: String,
        ownerId: String,
        ownerDisplayName: String,
        memberEmail: String
    ) {
        firestore.collection("project_invitations").document(invitationId).set(
            mapOf(
                "id" to invitationId,
                "projectId" to projectId,
                "projectTitle" to projectTitle,
                "inviterId" to ownerId,
                "inviterName" to ownerDisplayName,
                "inviteeId" to "",
                "inviteeEmail" to memberEmail,
                "status" to "PENDING",
                "createdAt" to Date()
            )
        ).await()
    }

    private fun ownerEmail(): String = auth.currentUser?.email.orEmpty()
}
