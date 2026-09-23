package com.proanimator.core.ink

/**
 * Optional Jetpack Ink bridge.
 *
 * Research (androidx.ink 1.0.0):
 * - ink-authoring-compose: InProgressStrokes
 * - ink-brush-compose: StockBrushes.pressurePen()
 * - ink-rendering / ink-strokes
 *
 * Full Ink authoring replaces the bitmap stroke path and needs
 * separate compositing into Flipbook frames. This module is a
 * **compile-optional** integration point:
 *
 * 1. Add dependencies in app/build.gradle.kts (see docs/JETPACK_INK.md)
 * 2. Set [InkFeatureFlags.enabled] = true
 * 3. Host InProgressStrokes overlay; onStrokesFinished → rasterize to layer bitmap
 *
 * Until deps are present, [isAvailable] is false and ProAnimator
 * uses BrushEngine + pressure strokes only.
 */
object InkFeatureFlags {
    /** Toggle at runtime after deps are on classpath */
    @Volatile
    var enabled: Boolean = false

    /**
     * True when androidx.ink classes can be loaded.
     * Checked via reflection so the app builds without ink deps.
     */
    val isAvailable: Boolean by lazy {
        try {
            Class.forName("androidx.ink.brush.StockBrushes")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    val canUse: Boolean get() = enabled && isAvailable
}

/**
 * Placeholder API for future full Ink overlay.
 * Implement against real androidx.ink when dependency is added.
 */
interface InkStrokeSink {
    fun onStrokeFinished(points: List<Pair<Float, Float>>, pressures: List<Float>)
}

object InkBridge {
    fun describe(): String {
        return when {
            InkFeatureFlags.canUse -> "Jetpack Ink ACTIVE"
            InkFeatureFlags.isAvailable -> "Jetpack Ink available (enable flag)"
            else -> "Jetpack Ink not on classpath — using BrushEngine"
        }
    }
}
