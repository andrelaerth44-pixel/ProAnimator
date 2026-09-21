package com.proanimator.core.brushes

import java.util.UUID

data class Brush(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: BrushCategory = BrushCategory.INK,
    val defaultSize: Float = 10f,
    val minSize: Float = 1f,
    val maxSize: Float = 200f,
    val defaultOpacity: Float = 1f,
    val pressureSize: Boolean = true,
    val pressureOpacity: Boolean = true,
    val stabilization: Float = 0.3f,
    val spacing: Float = 0.15f
)

enum class BrushCategory {
    SKETCH,
    INK,
    PAINT,
    AIRBRUSH,
    TEXTURE,
    ERASER
}

object DefaultBrushes {
    val technicalPen = Brush(
        name = "Technical Pen",
        category = BrushCategory.INK,
        defaultSize = 4f,
        pressureSize = true,
        pressureOpacity = false,
        stabilization = 0.4f
    )

    val softAirbrush = Brush(
        name = "Soft Airbrush",
        category = BrushCategory.AIRBRUSH,
        defaultSize = 40f,
        pressureOpacity = true,
        pressureSize = true,
        stabilization = 0.2f
    )

    val roundBrush = Brush(
        name = "Round Brush",
        category = BrushCategory.PAINT,
        defaultSize = 20f,
        pressureSize = true,
        pressureOpacity = true
    )

    val all = listOf(technicalPen, softAirbrush, roundBrush)
}
