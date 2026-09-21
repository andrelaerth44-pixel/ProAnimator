package com.proanimator.domain.model

import java.util.UUID

enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    HOLD
}

/**
 * A keyframe stores a value at a specific frame.
 * Supports common easing types used in Procreate Dreams.
 */
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val frame: Int,
    val value: Float,
    val easing: EasingType = EasingType.EASE_IN_OUT,
    // Future: for Bezier handles
    val inTangent: Float? = null,
    val outTangent: Float? = null
)

/**
 * Animatable property (position X/Y, scale, rotation, opacity, etc.)
 */
data class AnimatableProperty(
    val name: String,
    val keyframes: List<Keyframe> = emptyList()
) {
    fun valueAt(frame: Int): Float {
        if (keyframes.isEmpty()) return 0f
        if (keyframes.size == 1) return keyframes.first().value

        val sorted = keyframes.sortedBy { it.frame }

        // Before first keyframe
        if (frame <= sorted.first().frame) return sorted.first().value

        // After last keyframe
        if (frame >= sorted.last().frame) return sorted.last().value

        // Find the two keyframes we are between
        for (i in 0 until sorted.lastIndex) {
            val k1 = sorted[i]
            val k2 = sorted[i + 1]

            if (frame in k1.frame..k2.frame) {
                if (k1.easing == EasingType.HOLD) return k1.value

                val t = (frame - k1.frame).toFloat() / (k2.frame - k1.frame).toFloat()
                val easedT = applyEasing(t, k1.easing)
                return k1.value + (k2.value - k1.value) * easedT
            }
        }

        return sorted.last().value
    }

    private fun applyEasing(t: Float, type: EasingType): Float {
        return when (type) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> t * (2f - t)
            EasingType.EASE_IN_OUT -> {
                if (t < 0.5f) 2f * t * t
                else -1f + (4f - 2f * t) * t
            }
            EasingType.HOLD -> 0f // handled earlier
        }
    }
}
