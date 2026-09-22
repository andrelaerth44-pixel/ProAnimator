package com.proanimator.core.brushes

/**
 * Data-driven brush system inspired by:
 * - Jetpack Ink StockBrushes (pressure, marker, highlighter)
 * - SmartToolFactory Compose-Drawing-App path properties
 * - DrawBox pressure/tilt sampling patterns
 *
 * Draws onto ImageBitmap (compatible with true eraser + Flipbook export).
 */
data class BrushPreset(
    val id: String,
    val name: String,
    val baseSize: Float,
    val minSizeFactor: Float = 0.3f,
    val maxSizeFactor: Float = 1.2f,
    val opacity: Float = 1f,
    val hardness: Float = 1f,      // 1 = hard edge, 0 = soft
    val spacing: Float = 0.15f,      // relative to size for stamp spacing
    val pressureAffectsSize: Boolean = true,
    val pressureAffectsOpacity: Boolean = false,
    val smoothing: Float = 0.5f,     // 0 = raw, 1 = heavy smooth
    val isEraser: Boolean = false
) {
    fun sizeForPressure(pressure: Float): Float {
        val p = pressure.coerceIn(0f, 1f)
        return if (pressureAffectsSize) {
            baseSize * (minSizeFactor + (maxSizeFactor - minSizeFactor) * p)
        } else baseSize
    }

    fun opacityForPressure(pressure: Float): Float {
        val p = pressure.coerceIn(0f, 1f)
        return if (pressureAffectsOpacity) {
            opacity * (0.2f + 0.8f * p)
        } else opacity
    }
}

object BrushLibrary {
    val PEN = BrushPreset(
        id = "pen",
        name = "Pen",
        baseSize = 8f,
        minSizeFactor = 0.4f,
        maxSizeFactor = 1.1f,
        opacity = 1f,
        hardness = 1f,
        pressureAffectsSize = true,
        smoothing = 0.4f
    )

    val PENCIL = BrushPreset(
        id = "pencil",
        name = "Pencil",
        baseSize = 6f,
        minSizeFactor = 0.5f,
        maxSizeFactor = 1.0f,
        opacity = 0.75f,
        hardness = 0.85f,
        pressureAffectsSize = true,
        pressureAffectsOpacity = true,
        smoothing = 0.35f
    )

    val MARKER = BrushPreset(
        id = "marker",
        name = "Marker",
        baseSize = 22f,
        minSizeFactor = 0.85f,
        maxSizeFactor = 1.05f,
        opacity = 0.55f,
        hardness = 0.7f,
        pressureAffectsSize = false,
        pressureAffectsOpacity = true,
        smoothing = 0.55f
    )

    val SOFT = BrushPreset(
        id = "soft",
        name = "Soft Brush",
        baseSize = 28f,
        minSizeFactor = 0.6f,
        maxSizeFactor = 1.15f,
        opacity = 0.4f,
        hardness = 0.25f,
        pressureAffectsSize = true,
        pressureAffectsOpacity = true,
        smoothing = 0.65f
    )

    val INK = BrushPreset(
        id = "ink",
        name = "Ink",
        baseSize = 10f,
        minSizeFactor = 0.25f,
        maxSizeFactor = 1.4f,
        opacity = 1f,
        hardness = 0.95f,
        pressureAffectsSize = true,
        smoothing = 0.5f
    )

    val ERASER = BrushPreset(
        id = "eraser",
        name = "Eraser",
        baseSize = 24f,
        minSizeFactor = 1f,
        maxSizeFactor = 1f,
        opacity = 1f,
        hardness = 1f,
        pressureAffectsSize = false,
        smoothing = 0.3f,
        isEraser = true
    )

    val ALL = listOf(PEN, PENCIL, MARKER, SOFT, INK, ERASER)
}
