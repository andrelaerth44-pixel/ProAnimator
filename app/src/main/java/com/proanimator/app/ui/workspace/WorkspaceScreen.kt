package com.proanimator.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TEMPORARY stub after a failed large-file push.
 *
 * Restore full workspace:
 * ```
 * git checkout 98d1ff03 -- app/src/main/java/com/proanimator/app/ui/workspace/WorkspaceScreen.kt
 * ```
 * Then wire Frames:
 * - import FrameClipboard
 * - var showFrames + frameClipboard
 * - ToolButton("Frames")
 * - if (showFrames) FrameViewerSheet(...)
 *
 * Already on main: ActiveFramePool, FrameClipboard, FrameViewerSheet, Flipbook frame ops.
 */
@Composable
fun WorkspaceScreen() {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Restore WorkspaceScreen from git 98d1ff03\n" +
                "Frame Viewer + ActiveFramePool already on main",
            color = Color(0xFFBB86FC),
            fontSize = 14.sp
        )
    }
}
