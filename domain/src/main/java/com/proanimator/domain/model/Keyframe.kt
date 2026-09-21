package com.proanimator.domain.model

import java.util.UUID

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeFrame: Int,
    val properties: Map<String, Any> = emptyMap(),
    val easing: Easing = Easing.LINEAR
)

enum class Easing {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BEZIER
}
