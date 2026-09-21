package com.proanimator.domain.model

import java.util.UUID

/**
 * A single frame inside a Flipbook.
 * Holds the strokes drawn on that frame.
 */
data class FlipbookFrame(
    val id: String = UUID.randomUUID().toString(),
    val frameIndex: Int,
    val strokes: List<Stroke> = emptyList()
)

/**
 * Flipbook = traditional frame-by-frame animation container.
 * Similar to Procreate Dreams Flipbook / Animation Assist.
 */
data class Flipbook(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Flipbook",
    val frames: List<FlipbookFrame> = listOf(FlipbookFrame(frameIndex = 0)),
    val onionSkinEnabled: Boolean = true,
    val onionSkinBefore: Int = 2,   // how many previous frames to show
    val onionSkinAfter: Int = 1,    // how many next frames to show
    val onionSkinOpacity: Float = 0.3f
) {
    val frameCount: Int get() = frames.size

    fun frameAt(index: Int): FlipbookFrame? {
        return frames.getOrNull(index)
    }

    fun currentStrokes(index: Int): List<Stroke> {
        return frameAt(index)?.strokes ?: emptyList()
    }
}
