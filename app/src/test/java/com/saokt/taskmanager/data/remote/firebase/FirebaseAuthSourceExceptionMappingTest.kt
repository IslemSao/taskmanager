package com.saokt.taskmanager.data.remote.firebase

import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import io.mockk.mockk
import org.junit.Test

class FirebaseAuthSourceExceptionMappingTest {

    @Test
    fun `network exceptions map to connection guidance`() {
        val mapped = FirebaseAuthSource.mapAuthException(
            mockk<FirebaseNetworkException>(relaxed = true)
        )

        assertThat(mapped).isInstanceOf(IllegalStateException::class.java)
        assertThat(mapped.message).isEqualTo("Check your internet connection and try again.")
    }

    @Test
    fun `too many requests maps to throttling guidance`() {
        val mapped = FirebaseAuthSource.mapAuthException(
            mockk<FirebaseTooManyRequestsException>(relaxed = true)
        )

        assertThat(mapped).isInstanceOf(IllegalStateException::class.java)
        assertThat(mapped.message).isEqualTo("Too many attempts. Please wait and try again.")
    }

    @Test
    fun `recent login required maps to reauthentication guidance`() {
        val mapped = FirebaseAuthSource.mapAuthException(
            mockk<FirebaseAuthRecentLoginRequiredException>(relaxed = true)
        )

        assertThat(mapped).isInstanceOf(IllegalStateException::class.java)
        assertThat(mapped.message).isEqualTo("For security, please sign in again before deleting your account.")
    }

    @Test
    fun `other exceptions pass through unchanged`() {
        val original = IllegalArgumentException("Original failure")

        val mapped = FirebaseAuthSource.mapAuthException(original)

        assertThat(mapped).isSameInstanceAs(original)
    }
}
