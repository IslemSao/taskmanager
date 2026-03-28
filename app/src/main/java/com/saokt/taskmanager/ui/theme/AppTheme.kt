package com.saokt.taskmanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-wide spacing and shape tokens so screens stay visually consistent.
 */
object AppTheme {
    val screenPadding: Dp = 20.dp
    val sectionSpacing: Dp = 20.dp
    val cardShape: Shape = RoundedCornerShape(24.dp)
    val chipShape: Shape = RoundedCornerShape(999.dp)
}
