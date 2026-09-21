package com.proanimator.core.timeline

import com.proanimator.domain.model.Flipbook
import com.proanimator.domain.model.FlipbookFrame
import com.proanimator.domain.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages Flipbook state and Onion Skin with classic colors.
 * Previous frames = Red/Orange
 * Next frames = Green/Cyan
 */
class FlipbookEngine {

    private val _flipbook = MutableStateFlow(Flipbook(name = "Main Flipbook"))
    val flipbook: StateFlow<Flipbook> = _flipbook.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    val currentFrame: FlipbookFrame?
        get() = _flipbook.value.frameAt(_currentFrameIndex.value)

    fun setCurrentFrame(index: Int) {
        val max = (_flipbook.value.frameCount - 1).coerceAtLeast(0)
        _currentFrameIndex.value = index.coerceIn(0, max)
    }

    fun nextFrame() {
        val next = _currentFrameIndex.value + 1
        if (next >= _flipbook.value.frameCount) {
            addFrame()
            _currentFrameIndex.value = _flipbook.value.frameCount - 1
        } else {
            _currentFrameIndex.value = next
        }
    }

    fun previousFrame() {
        setCurrentFrame(_currentFrameIndex.value - 1)
    }

    fun addFrame() {
        _flipbook.update { current ->
            val newIndex = current.frames.size
            current.copy(frames = current.frames + FlipbookFrame(frameIndex = newIndex))
        }
    }

    fun duplicateCurrentFrame() {
        val current = currentFrame ?: return
        _flipbook.update { fb ->
            val newIndex = fb.frames.size
            val duplicated = current.copy(id = java.util.UUID.randomUUID().toString(), frameIndex = newIndex)
            fb.copy(frames = fb.frames + duplicated)
        }
        _currentFrameIndex.value = _flipbook.value.frameCount - 1
    }

    fun deleteCurrentFrame() {
        if (_flipbook.value.frameCount <= 1) return
        _flipbook.update { fb ->
            val newFrames = fb.frames
                .filterIndexed { index, _ -> index != _currentFrameIndex.value }
                .mapIndexed { index, frame -> frame.copy(frameIndex = index) }
            fb.copy(frames = newFrames)
        }
        if (_currentFrameIndex.value >= _flipbook.value.frameCount) {
            _currentFrameIndex.value = _flipbook.value.frameCount - 1
        }
    }

    fun addStrokeToCurrentFrame(stroke: Stroke) {
        val index = _currentFrameIndex.value
        _flipbook.update { fb ->
            val newFrames = fb.frames.mapIndexed { i, frame ->
                if (i == index) frame.copy(strokes = frame.strokes + stroke) else frame
            }
            fb.copy(frames = newFrames)
        }
    }

    fun clearCurrentFrame() {
        val index = _currentFrameIndex.value
        _flipbook.update { fb ->
            val newFrames = fb.frames.mapIndexed { i, frame ->
                if (i == index) frame.copy(strokes = emptyList()) else frame
            }
            fb.copy(frames = newFrames)
        }
    }

    fun setOnionSkinEnabled(enabled: Boolean) {
        _flipbook.update { it.copy(onionSkinEnabled = enabled) }
    }

    fun setOnionSkinRange(before: Int, after: Int) {
        _flipbook.update {
            it.copy(
                onionSkinBefore = before.coerceIn(0, 5),
                onionSkinAfter = after.coerceIn(0, 5)
            )
        }
    }

    /**
     * Returns onion skin data with classic colors:
     * - Previous frames → Red / Orange tones
     * - Next frames → Green / Cyan tones
     */
    data class OnionSkinLayer(
        val strokes: List<Stroke>,
        val color: Long,      // tint color
        val opacity: Float
    )

    fun getOnionSkinLayers(): List<OnionSkinLayer> {
        val fb = _flipbook.value
        if (!fb.onionSkinEnabled) return emptyList()

        val result = mutableListOf<OnionSkinLayer>()
        val current = _currentFrameIndex.value

        // Previous frames (Red family)
        val prevColors = listOf(0xFFFF5252, 0xFFFF8A65, 0xFFFFAB91) // red → orange
        for (i in 1..fb.onionSkinBefore) {
            val idx = current - i
            if (idx >= 0) {
                val opacity = fb.onionSkinOpacity * (1f - (i - 1) * 0.22f)
                val color = prevColors.getOrElse(i - 1) { 0xFFFF5252 }
                result.add(OnionSkinLayer(fb.frames[idx].strokes, color, opacity.coerceIn(0.12f, 0.45f)))
            }
        }

        // Next frames (Green family)
        val nextColors = listOf(0xFF69F0AE, 0xFF00E676, 0xFF00C853) // cyan-green → green
        for (i in 1..fb.onionSkinAfter) {
            val idx = current + i
            if (idx < fb.frames.size) {
                val opacity = fb.onionSkinOpacity * 0.55f * (1f - (i - 1) * 0.25f)
                val color = nextColors.getOrElse(i - 1) { 0xFF69F0AE }
                result.add(OnionSkinLayer(fb.frames[idx].strokes, color, opacity.coerceIn(0.1f, 0.35f)))
            }
        }

        return result
    }
}
