package com.proanimator.domain.model

import java.util.UUID

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val isAlphaLocked: Boolean = false
)

enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    HARD_LIGHT,
    SOFT_LIGHT,
    DIFFERENCE,
    EXCLUSION
}
