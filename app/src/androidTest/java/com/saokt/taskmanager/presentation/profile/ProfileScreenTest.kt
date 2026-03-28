package com.saokt.taskmanager.presentation.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saokt.taskmanager.domain.model.User
import com.saokt.taskmanager.testing.runWithScreenshotOnFailure
import com.saokt.taskmanager.ui.theme.TaskmanagerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun profileScreen_loadingStateShowsSpinner() = composeRule.runWithScreenshotOnFailure("profile_loading") {
        composeRule.setContent {
            TaskmanagerTheme {
                ProfileScreenContent(
                    uiState = ProfileUiState(isLoading = true),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    onSendVerificationEmailClick = {},
                    onRefreshVerificationClick = {},
                    onSignOutClick = {},
                    onDeleteAccountClick = {}
                )
            }
        }

        composeRule.onNodeWithTag(ProfileTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun profileScreen_unverifiedEmailShowsVerificationActions() = composeRule.runWithScreenshotOnFailure("profile_unverified") {
        var sendClicks = 0
        var refreshClicks = 0
        var deleteClicks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                ProfileScreenContent(
                    uiState = ProfileUiState(
                        isLoading = false,
                        user = User(id = "u1", email = "jane@example.com", displayName = "Jane"),
                        isEmailVerified = false
                    ),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    onSendVerificationEmailClick = { sendClicks += 1 },
                    onRefreshVerificationClick = { refreshClicks += 1 },
                    onSignOutClick = {},
                    onDeleteAccountClick = { deleteClicks += 1 }
                )
            }
        }

        composeRule.onNodeWithTag(ProfileTestTags.USER_INFO_CARD).assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTestTags.VERIFICATION_STATUS).assertIsDisplayed()
        composeRule.onNodeWithText("Not verified").assertIsDisplayed()
        composeRule.onNodeWithTag(ProfileTestTags.SEND_VERIFICATION_BUTTON).performClick()
        composeRule.onNodeWithTag(ProfileTestTags.REFRESH_VERIFICATION_BUTTON).performClick()
        composeRule.onNodeWithTag(ProfileTestTags.DELETE_ACCOUNT_BUTTON).performClick()

        assertEquals(1, sendClicks)
        assertEquals(1, refreshClicks)
        assertEquals(1, deleteClicks)
    }

    @Test
    fun profileScreen_verifiedEmailHidesVerificationActions() = composeRule.runWithScreenshotOnFailure("profile_verified") {
        composeRule.setContent {
            TaskmanagerTheme {
                ProfileScreenContent(
                    uiState = ProfileUiState(
                        isLoading = false,
                        user = User(id = "u1", email = "jane@example.com", displayName = "Jane"),
                        isEmailVerified = true
                    ),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    onSendVerificationEmailClick = {},
                    onRefreshVerificationClick = {},
                    onSignOutClick = {},
                    onDeleteAccountClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Verified").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ProfileTestTags.SEND_VERIFICATION_BUTTON).assertCountEquals(0)
        composeRule.onAllNodesWithTag(ProfileTestTags.REFRESH_VERIFICATION_BUTTON).assertCountEquals(0)
    }

    @Test
    fun profileScreen_sendingVerificationDisablesVerificationActions() = composeRule.runWithScreenshotOnFailure("profile_verification_loading") {
        composeRule.setContent {
            TaskmanagerTheme {
                ProfileScreenContent(
                    uiState = ProfileUiState(
                        isLoading = false,
                        user = User(id = "u1", email = "jane@example.com", displayName = "Jane"),
                        isEmailVerified = false,
                        isSendingVerificationEmail = true
                    ),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    onSendVerificationEmailClick = {},
                    onRefreshVerificationClick = {},
                    onSignOutClick = {},
                    onDeleteAccountClick = {}
                )
            }
        }

        composeRule.onNodeWithTag(ProfileTestTags.SEND_VERIFICATION_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(ProfileTestTags.REFRESH_VERIFICATION_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun profileScreen_deleteLoadingShowsDeletingState() = composeRule.runWithScreenshotOnFailure("profile_delete_loading") {
        composeRule.setContent {
            TaskmanagerTheme {
                ProfileScreenContent(
                    uiState = ProfileUiState(
                        isLoading = false,
                        isDeletingAccount = true,
                        user = User(id = "u1", email = "jane@example.com")
                    ),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    onSendVerificationEmailClick = {},
                    onRefreshVerificationClick = {},
                    onSignOutClick = {},
                    onDeleteAccountClick = {}
                )
            }
        }

        composeRule.onNodeWithTag(ProfileTestTags.DELETE_LOADING).assertIsDisplayed()
        composeRule.onNodeWithText("Deleting account...").assertIsDisplayed()
    }

    @Test
    fun profileScreen_messageShowsInSnackbar() = composeRule.runWithScreenshotOnFailure("profile_message_snackbar") {
        composeRule.setContent {
            TaskmanagerTheme {
                ProfileScreenContent(
                    uiState = ProfileUiState(
                        isLoading = false,
                        user = User(id = "u1", email = "jane@example.com"),
                        message = "Verification email sent"
                    ),
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    onSendVerificationEmailClick = {},
                    onRefreshVerificationClick = {},
                    onSignOutClick = {},
                    onDeleteAccountClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Verification email sent").assertIsDisplayed()
    }
}
