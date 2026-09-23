package com.proanimator.core.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.min

/**
 * Viewport for zoom/pan over the drawing surface.
 */
class CanvasViewport(
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    var scale: Float = 1f
        private set
    var offset: Offset = Offset.Zero
        private set

    val minScale = 0.05f
    val maxScale = 8f

    fun applyZoomPan(centroid: Offset, pan: Offset, zoom: Float) {
        val oldScale = scale
        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
        offset = (offset + centroid / oldScale) - (centroid / newScale + pan / oldScale)
        scale = newScale
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /**
     * Fit entire canvas into [viewWidth] x [viewHeight] with letterboxing padding.
     * Call after load / on size change.
     */
    fun fitToScreen(viewWidth: Float, viewHeight: Float, padding: Float = 24f) {
        if (viewWidth <= 0f || viewHeight <= 0f) return
        val availW = (viewWidth - padding * 2).coerceAtLeast(1f)
        val availH = (viewHeight - padding * 2).coerceAtLeast(1f)
        val sx = availW / canvasWidth
        val sy = availH / canvasHeight
        scale = min(sx, sy).coerceIn(minScale, maxScale)
        // Center canvas in view (in canvas-space offset used by withTransform)
        // screen = (canvas + offset) * scale  → center:
        // view/2 = (canvasCenter + offset) * scale
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f
        offset = Offset(
            viewWidth / (2f * scale) - cx,
            viewHeight / (2f * scale) - cy
        )
    }

    fun screenToCanvas(screen: Offset): Offset = (screen / scale) - offset

    fun canvasToScreen(canvas: Offset): Offset = (canvas + offset) * scale
}
