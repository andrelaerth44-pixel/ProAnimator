package com.proanimator.domain.model

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Stroke(
    val points: List<StrokePoint>,
    val brushId: String,
    val color: Long,
    val size: Float,
    val opacity: Float = 1f
)
