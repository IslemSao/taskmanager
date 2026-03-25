package com.saokt.taskmanager.testing

import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ComposeUiScreenshot"

fun ComposeTestRule.runWithScreenshotOnFailure(
    screenshotName: String,
    block: () -> Unit
) {
    try {
        block()
    } catch (throwable: Throwable) {
        saveScreenshot(screenshotName)
        throw throwable
    }
}

fun ComposeTestRule.saveScreenshot(screenshotName: String): File? {
    waitForIdle()

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val outputDir = context.getExternalFilesDir("test-screenshots") ?: return null
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    val sanitizedName = screenshotName
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "ui_test" }
    val outputFile = File(outputDir, "${System.currentTimeMillis()}_${sanitizedName}.png")

    FileOutputStream(outputFile).use { outputStream ->
        onRoot(useUnmergedTree = true)
            .captureToImage()
            .asAndroidBitmap()
            .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
    }

    Log.e(TAG, "Saved UI test screenshot to ${outputFile.absolutePath}")
    return outputFile
}
