package com.saokt.taskmanager.presentation.authentication.signin

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saokt.taskmanager.testing.runWithScreenshotOnFailure
import com.saokt.taskmanager.ui.theme.TaskmanagerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signInScreen_rendersExpectedFields() = composeRule.runWithScreenshotOnFailure("sign_in_renders") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignInTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.SUBTITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.EMAIL_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.SUBMIT_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.FORGOT_PASSWORD_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.GOOGLE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.SIGN_UP_BUTTON).assertIsDisplayed()
    }

    @Test
    fun signInScreen_updatesTypedCredentialsAndHandlesClicks() = composeRule.runWithScreenshotOnFailure("sign_in_interactions") {
        var uiState by mutableStateOf(SignInState())
        var signInClicks = 0
        var forgotPasswordClicks = 0
        var googleClicks = 0
        var signUpClicks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = uiState,
                    onEmailChanged = { uiState = uiState.copy(email = it) },
                    onPasswordChanged = { uiState = uiState.copy(password = it) },
                    onSignInClick = { signInClicks += 1 },
                    onForgotPasswordClick = { forgotPasswordClicks += 1 },
                    onGoogleSignInClick = { googleClicks += 1 },
                    onSignUpClick = { signUpClicks += 1 },
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignInTestTags.EMAIL_FIELD).performTextInput("user@example.com")
        composeRule.onNodeWithTag(SignInTestTags.PASSWORD_FIELD).performTextInput("secret123")
        composeRule.onNodeWithTag(SignInTestTags.SUBMIT_BUTTON).performClick()
        composeRule.onNodeWithTag(SignInTestTags.FORGOT_PASSWORD_BUTTON).performClick()
        composeRule.onNodeWithTag(SignInTestTags.GOOGLE_BUTTON).performClick()
        composeRule.onNodeWithTag(SignInTestTags.SIGN_UP_BUTTON).performClick()

        composeRule.onNodeWithTag(SignInTestTags.EMAIL_FIELD).assertTextContains("user@example.com")
        assertEquals(1, signInClicks)
        assertEquals(1, forgotPasswordClicks)
        assertEquals(1, googleClicks)
        assertEquals(1, signUpClicks)
    }

    @Test
    fun signInScreen_showsValidationErrorFromState() = composeRule.runWithScreenshotOnFailure("sign_in_error_state") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(error = "Email and password cannot be empty"),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithText("Email and password cannot be empty").assertIsDisplayed()
    }

    @Test
    fun signInScreen_loadingStateDisablesPrimaryActions() = composeRule.runWithScreenshotOnFailure("sign_in_loading_state") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(isLoading = true),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignInTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(SignInTestTags.GOOGLE_BUTTON).assertIsNotEnabled()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun signInScreen_passwordToggleUpdatesAccessibilityLabel() = composeRule.runWithScreenshotOnFailure("sign_in_password_toggle") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignInTestTags.PASSWORD_TOGGLE)
            .assertContentDescriptionContains("Show password")
            .performClick()
        composeRule.onNodeWithTag(SignInTestTags.PASSWORD_TOGGLE)
            .assertContentDescriptionContains("Hide password")
    }

    @Test
    fun signInScreen_callsOnSignedInWhenStateBecomesSignedIn() = composeRule.runWithScreenshotOnFailure("sign_in_signed_in_callback") {
        var signedInCallbacks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(isSignedIn = true),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = { signedInCallbacks += 1 }
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(1, signedInCallbacks)
    }

    @Test
    fun signInScreen_callsOnErrorShownAfterDisplayingSnackbar() = composeRule.runWithScreenshotOnFailure("sign_in_error_callback") {
        var errorShownCallbacks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(error = "Wrong password"),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = { errorShownCallbacks += 1 },
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithText("Wrong password").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, errorShownCallbacks)
    }

    @Test
    fun signInScreen_acceptsTrimmedEmailInput() = composeRule.runWithScreenshotOnFailure("sign_in_trimmed_input") {
        var uiState by mutableStateOf(SignInState())

        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = uiState,
                    onEmailChanged = { uiState = uiState.copy(email = it) },
                    onPasswordChanged = { uiState = uiState.copy(password = it) },
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignInTestTags.EMAIL_FIELD).performTextInput("  jane@example.com  ")
        composeRule.onNodeWithTag(SignInTestTags.PASSWORD_FIELD).performTextInput("secret123")

        composeRule.onNodeWithTag(SignInTestTags.EMAIL_FIELD).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                androidx.compose.ui.text.AnnotatedString("  jane@example.com  ")
            )
        )
        composeRule.onNodeWithTag(SignInTestTags.PASSWORD_FIELD).assertIsDisplayed()
    }

    @Test
    fun signInScreen_clearsShownErrorWhenUserEditsField() = composeRule.runWithScreenshotOnFailure("sign_in_edit_clears_error") {
        var uiState by mutableStateOf(SignInState(error = "Wrong password"))

        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = uiState,
                    onEmailChanged = { uiState = uiState.copy(email = it, error = null) },
                    onPasswordChanged = { uiState = uiState.copy(password = it, error = null) },
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = {},
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithText("Wrong password").assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTestTags.EMAIL_FIELD).performTextInput("j")
        composeRule.waitForIdle()
    }

    @Test
    fun signInScreen_callsOnMessageShownAfterDisplayingSnackbar() = composeRule.runWithScreenshotOnFailure("sign_in_message_callback") {
        var messageShownCallbacks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignInScreenContent(
                    state = SignInState(message = "Password reset email sent"),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSignInClick = {},
                    onForgotPasswordClick = {},
                    onGoogleSignInClick = {},
                    onSignUpClick = {},
                    onErrorShown = {},
                    onMessageShown = { messageShownCallbacks += 1 },
                    onSignedIn = {}
                )
            }
        }

        composeRule.onNodeWithText("Password reset email sent").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, messageShownCallbacks)
    }
}
