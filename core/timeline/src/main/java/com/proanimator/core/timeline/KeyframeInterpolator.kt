package com.proanimator.core.timeline

import kotlin.math.pow

/**
 * Keyframe interpolation with cubic Bezier easing.
 * Based on CSS CubicBezierEasing + Compose animation-core research.
 */
enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    HOLD,
    BEZIER
}

data class Keyframe(
    val frame: Int,
    val value: Float,
    val easing: EasingType = EasingType.EASE_IN_OUT,
    /** Bezier control points (x1,y1,x2,y2) in 0..1 — used when easing == BEZIER */
    val bx1: Float = 0.42f,
    val by1: Float = 0f,
    val bx2: Float = 0.58f,
    val by2: Float = 1f
)

enum class AnimProperty {
    POS_X, POS_Y, SCALE, ROTATION, OPACITY
}

data class PropertyTrack(
    val property: AnimProperty,
    val keyframes: MutableList<Keyframe> = mutableListOf()
)

object KeyframeInterpolator {

    fun interpolate(track: PropertyTrack, frame: Float, default: Float = 0f): Float {
        val kfs = track.keyframes.sortedBy { it.frame }
        if (kfs.isEmpty()) return default
        if (frame <= kfs.first().frame) return kfs.first().value
        if (frame >= kfs.last().frame) return kfs.last().value

        for (i in 0 until kfs.size - 1) {
            val a = kfs[i]
            val b = kfs[i + 1]
            if (frame >= a.frame && frame <= b.frame) {
                if (a.easing == EasingType.HOLD) return a.value
                val span = (b.frame - a.frame).toFloat().coerceAtLeast(1f)
                val t = ((frame - a.frame) / span).coerceIn(0f, 1f)
                val eased = ease(t, a)
                return a.value + (b.value - a.value) * eased
            }
        }
        return default
    }

    private fun ease(t: Float, kf: Keyframe): Float {
        return when (kf.easing) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> t * (2f - t)
            EasingType.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
            EasingType.HOLD -> 0f
            EasingType.BEZIER -> cubicBezierY(t, kf.bx1, kf.by1, kf.bx2, kf.by2)
        }
    }

    /**
     * Solve cubic Bezier for y given x=t (CSS-style).
     * Control points: (0,0), (x1,y1), (x2,y2), (1,1)
     */
    fun cubicBezierY(t: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        // Newton-Raphson to find s such that Bx(s) ≈ t, then return By(s)
        var s = t
        repeat(8) {
            val x = bezierCoord(s, x1, x2)
            val dx = bezierDerivative(s, x1, x2)
            if (kotlin.math.abs(dx) < 1e-6f) return@repeat
            s = (s - (x - t) / dx).coerceIn(0f, 1f)
        }
        return bezierCoord(s, y1, y2)
    }

    private fun bezierCoord(s: Float, c1: Float, c2: Float): Float {
        // B(s) = 3(1-s)^2 s * c1 + 3(1-s) s^2 * c2 + s^3
        val u = 1f - s
        return 3f * u * u * s * c1 + 3f * u * s * s * c2 + s * s * s
    }

    private fun bezierDerivative(s: Float, c1: Float, c2: Float): Float {
        val u = 1f - s
        return 3f * u * u * c1 + 6f * u * s * (c2 - c1) + 3f * s * s * (1f - c2)
    }

    /** Common CSS-like presets as Bezier keyframes helpers */
    fun easeInCubic() = Keyframe(0, 0f, EasingType.BEZIER, 0.55f, 0.055f, 0.675f, 0.19f)
    fun easeOutCubic() = Keyframe(0, 0f, EasingType.BEZIER, 0.215f, 0.61f, 0.355f, 1f)
    fun easeInOutCubic() = Keyframe(0, 0f, EasingType.BEZIER, 0.645f, 0.045f, 0.355f, 1f)
}
