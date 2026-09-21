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
        id = "technical_pen",
        name = "Technical Pen",
        category = BrushCategory.INK,
        defaultSize = 4f,
        pressureSize = true,
        pressureOpacity = false,
        stabilization = 0.45f
    )

    val softAirbrush = Brush(
        id = "soft_airbrush",
        name = "Soft Airbrush",
        category = BrushCategory.AIRBRUSH,
        defaultSize = 40f,
        pressureOpacity = true,
        pressureSize = true,
        stabilization = 0.2f
    )

    val roundBrush = Brush(
        id = "round_brush",
        name = "Round Brush",
        category = BrushCategory.PAINT,
        defaultSize = 18f,
        pressureSize = true,
        pressureOpacity = true,
        stabilization = 0.3f
    )

    val sketchPencil = Brush(
        id = "sketch_pencil",
        name = "Sketch Pencil",
        category = BrushCategory.SKETCH,
        defaultSize = 6f,
        pressureSize = true,
        pressureOpacity = true,
        stabilization = 0.15f
    )

    val all = listOf(technicalPen, softAirbrush, roundBrush, sketchPencil)
}
