package com.proanimator.core.timeline

import com.proanimator.domain.model.Flipbook
import com.proanimator.domain.model.FlipbookFrame
import com.proanimator.domain.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages Flipbook state and Onion Skin.
 */
class FlipbookEngine {

    private val _flipbook = MutableStateFlow(
        Flipbook(name = "Main Flipbook")
    )
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
            // Auto-add new frame when going past the end
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
            val newFrame = FlipbookFrame(frameIndex = newIndex)
            current.copy(frames = current.frames + newFrame)
        }
    }

    fun duplicateCurrentFrame() {
        val current = currentFrame ?: return
        _flipbook.update { fb ->
            val newIndex = fb.frames.size
            val duplicated = current.copy(
                id = java.util.UUID.randomUUID().toString(),
                frameIndex = newIndex
            )
            fb.copy(frames = fb.frames + duplicated)
        }
        _currentFrameIndex.value = _flipbook.value.frameCount - 1
    }

    fun deleteCurrentFrame() {
        if (_flipbook.value.frameCount <= 1) return

        _flipbook.update { fb ->
            val newFrames = fb.frames.filterIndexed { index, _ -> index != _currentFrameIndex.value }
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
                if (i == index) {
                    frame.copy(strokes = frame.strokes + stroke)
                } else frame
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

    /** Returns strokes that should be drawn as onion skin (previous + next frames) */
    fun getOnionSkinStrokes(): List<Pair<List<Stroke>, Float>> {
        val fb = _flipbook.value
        if (!fb.onionSkinEnabled) return emptyList()

        val result = mutableListOf<Pair<List<Stroke>, Float>>()
        val current = _currentFrameIndex.value

        // Previous frames (older = more transparent)
        for (i in 1..fb.onionSkinBefore) {
            val idx = current - i
            if (idx >= 0) {
                val opacity = fb.onionSkinOpacity * (1f - (i - 1) * 0.25f)
                result.add(fb.frames[idx].strokes to opacity.coerceIn(0.1f, 0.5f))
            }
        }

        // Next frames
        for (i in 1..fb.onionSkinAfter) {
            val idx = current + i
            if (idx < fb.frames.size) {
                val opacity = fb.onionSkinOpacity * 0.6f * (1f - (i - 1) * 0.3f)
                result.add(fb.frames[idx].strokes to opacity.coerceIn(0.08f, 0.35f))
            }
        }

        return result
    }
}
