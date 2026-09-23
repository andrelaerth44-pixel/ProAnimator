package com.proanimator.app.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.proanimator.core.ink.InkBridge
import com.proanimator.core.ink.InkFeatureFlags
import com.proanimator.core.ink.InkRasterizer
import com.proanimator.core.timeline.FlipbookBitmapEngine

/**
 * Jetpack Ink overlay host.
 *
 * When androidx.ink is on the classpath and [InkFeatureFlags.enabled]:
 * replace the placeholder with real InProgressStrokes from
 * ink-authoring-compose (see docs/JETPACK_INK.md).
 *
 * Finished strokes are rasterized via [InkRasterizer] into the active layer.
 */
@Composable
fun InkComposeOverlay(
    flipbook: FlipbookBitmapEngine,
    color: Long,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled || !InkFeatureFlags.canUse) return

    // Placeholder until ink-authoring-compose is linked:
    // InProgressStrokes(
    //   modifier = modifier.fillMaxSize(),
    //   brushFamily = StockBrushes.pressurePen(),
    //   onStrokesFinished = { strokes ->
    //     // convert stroke geometry → InkRasterizer.Point list
    //     // flipbook.brushEngine path or InkRasterizer.rasterize(active.bitmap, …)
    //   }
    // )

    Box(modifier = modifier.fillMaxSize()) {
        Text(
            InkBridge.describe(),
            color = Color(0xFFBB86FC).copy(alpha = 0.5f),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/** Helper: push finished points into active layer (Ink finish path). */
fun applyInkStrokeToFlipbook(
    flipbook: FlipbookBitmapEngine,
    points: List<InkRasterizer.Point>,
    color: Long,
    baseWidth: Float = 8f,
    isEraser: Boolean = false
) {
    val frame = flipbook.currentFrame ?: return
    val active = frame.layers.activeLayer()
    InkRasterizer.rasterize(active.bitmap, points, color, baseWidth, isEraser)
    flipbook.bumpLayers()
}
