package com.proanimator.core.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min

/**
 * Viewport for zoom/pan over the drawing surface.
 * Screen coords → canvas (bitmap) coords conversion.
 *
 * Research: Compose transformable + detectTransformGestures
 * (centroid-aware zoom so pinch zooms toward fingers).
 */
class CanvasViewport(
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    var scale: Float = 1f
        private set
    var offset: Offset = Offset.Zero
        private set

    val minScale = 0.2f
    val maxScale = 8f

    fun applyZoomPan(centroid: Offset, pan: Offset, zoom: Float) {
        val oldScale = scale
        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
        // Zoom toward centroid
        offset = (offset + centroid / oldScale) - (centroid / newScale + pan / oldScale)
        scale = newScale
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** Screen position → position on the ImageBitmap */
    fun screenToCanvas(screen: Offset): Offset {
        return (screen / scale) - offset
    }

    fun canvasToScreen(canvas: Offset): Offset {
        return (canvas + offset) * scale
    }
}
