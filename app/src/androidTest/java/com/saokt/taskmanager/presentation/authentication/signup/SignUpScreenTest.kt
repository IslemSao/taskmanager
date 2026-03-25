package com.saokt.taskmanager.presentation.authentication.signup

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.SemanticsMatcher
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
class SignUpScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signUpScreen_rendersExpectedFields() = composeRule.runWithScreenshotOnFailure("sign_up_renders") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = SignUpState(),
                    onDisplayNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignUpTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.SUBTITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.DISPLAY_NAME_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.EMAIL_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.CONFIRM_PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.SUBMIT_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.SIGN_IN_BUTTON).assertIsDisplayed()
    }

    @Test
    fun signUpScreen_updatesTypedValuesAndHandlesClicks() = composeRule.runWithScreenshotOnFailure("sign_up_interactions") {
        var uiState by mutableStateOf(SignUpState())
        var submitClicks = 0
        var signInClicks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = uiState,
                    onDisplayNameChanged = { uiState = uiState.copy(displayName = it) },
                    onEmailChanged = { uiState = uiState.copy(email = it) },
                    onPasswordChanged = { uiState = uiState.copy(password = it) },
                    onConfirmPasswordChanged = { uiState = uiState.copy(confirmPassword = it) },
                    onSignUpClick = { submitClicks += 1 },
                    onSignInClick = { signInClicks += 1 },
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignUpTestTags.DISPLAY_NAME_FIELD).performTextInput("Jane Doe")
        composeRule.onNodeWithTag(SignUpTestTags.EMAIL_FIELD).performTextInput("jane@example.com")
        composeRule.onNodeWithTag(SignUpTestTags.PASSWORD_FIELD).performTextInput("secret123")
        composeRule.onNodeWithTag(SignUpTestTags.CONFIRM_PASSWORD_FIELD).performTextInput("secret123")
        composeRule.onNodeWithTag(SignUpTestTags.SUBMIT_BUTTON).performClick()
        composeRule.onNodeWithTag(SignUpTestTags.SIGN_IN_BUTTON).performClick()

        composeRule.onNodeWithTag(SignUpTestTags.DISPLAY_NAME_FIELD).assertTextContains("Jane Doe")
        composeRule.onNodeWithTag(SignUpTestTags.EMAIL_FIELD).assertTextContains("jane@example.com")
        assertEquals(1, submitClicks)
        assertEquals(1, signInClicks)
    }

    @Test
    fun signUpScreen_showsValidationErrorFromState() = composeRule.runWithScreenshotOnFailure("sign_up_error_state") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = SignUpState(error = "Passwords do not match"),
                    onDisplayNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun signUpScreen_loadingStateDisablesSubmitAndShowsIndicator() = composeRule.runWithScreenshotOnFailure("sign_up_loading") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = SignUpState(isLoading = true),
                    onDisplayNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignUpTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun signUpScreen_passwordTogglesUpdateAccessibilityLabel() = composeRule.runWithScreenshotOnFailure("sign_up_password_toggles") {
        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = SignUpState(),
                    onDisplayNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignUpTestTags.PASSWORD_TOGGLE)
            .assertContentDescriptionContains("Show password")
            .performClick()
        composeRule.onNodeWithTag(SignUpTestTags.PASSWORD_TOGGLE)
            .assertContentDescriptionContains("Hide password")

        composeRule.onNodeWithTag(SignUpTestTags.CONFIRM_PASSWORD_TOGGLE)
            .assertContentDescriptionContains("Show password")
            .performClick()
        composeRule.onNodeWithTag(SignUpTestTags.CONFIRM_PASSWORD_TOGGLE)
            .assertContentDescriptionContains("Hide password")
    }

    @Test
    fun signUpScreen_callsOnSignedUpWhenStateBecomesSignedUp() = composeRule.runWithScreenshotOnFailure("sign_up_signed_up_callback") {
        var signedUpCallbacks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = SignUpState(isSignedUp = true),
                    onDisplayNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = { signedUpCallbacks += 1 }
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(1, signedUpCallbacks)
    }

    @Test
    fun signUpScreen_callsOnErrorShownAfterDisplayingSnackbar() = composeRule.runWithScreenshotOnFailure("sign_up_error_callback") {
        var errorShownCallbacks = 0

        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = SignUpState(error = "All fields are required"),
                    onDisplayNameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = { errorShownCallbacks += 1 },
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithText("All fields are required").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, errorShownCallbacks)
    }

    @Test
    fun signUpScreen_acceptsTrimmedAndUnicodeInput() = composeRule.runWithScreenshotOnFailure("sign_up_unicode_and_trimmed_input") {
        var uiState by mutableStateOf(SignUpState())

        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = uiState,
                    onDisplayNameChanged = { uiState = uiState.copy(displayName = it) },
                    onEmailChanged = { uiState = uiState.copy(email = it) },
                    onPasswordChanged = { uiState = uiState.copy(password = it) },
                    onConfirmPasswordChanged = { uiState = uiState.copy(confirmPassword = it) },
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithTag(SignUpTestTags.DISPLAY_NAME_FIELD).performTextInput("  محمد علي  ")
        composeRule.onNodeWithTag(SignUpTestTags.EMAIL_FIELD).performTextInput("  jane@example.com  ")

        composeRule.onNodeWithTag(SignUpTestTags.DISPLAY_NAME_FIELD).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                androidx.compose.ui.text.AnnotatedString("  محمد علي  ")
            )
        )
        composeRule.onNodeWithTag(SignUpTestTags.EMAIL_FIELD).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                androidx.compose.ui.text.AnnotatedString("  jane@example.com  ")
            )
        )
    }

    @Test
    fun signUpScreen_clearsShownErrorWhenUserEditsField() = composeRule.runWithScreenshotOnFailure("sign_up_edit_clears_error") {
        var uiState by mutableStateOf(SignUpState(error = "All fields are required"))

        composeRule.setContent {
            TaskmanagerTheme {
                SignUpScreenContent(
                    state = uiState,
                    onDisplayNameChanged = { uiState = uiState.copy(displayName = it, error = null) },
                    onEmailChanged = { uiState = uiState.copy(email = it, error = null) },
                    onPasswordChanged = { uiState = uiState.copy(password = it, error = null) },
                    onConfirmPasswordChanged = { uiState = uiState.copy(confirmPassword = it, error = null) },
                    onSignUpClick = {},
                    onSignInClick = {},
                    onErrorShown = {},
                    onSignedUp = {}
                )
            }
        }

        composeRule.onNodeWithText("All fields are required").assertIsDisplayed()
        composeRule.onNodeWithTag(SignUpTestTags.EMAIL_FIELD).performTextInput("j")
        composeRule.waitForIdle()
    }
}
