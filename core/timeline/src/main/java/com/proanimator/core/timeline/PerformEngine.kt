package com.proanimator.core.timeline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Perform mode — record live transforms while scrubbing/playing.
 * Properties: POS_X, POS_Y, SCALE, ROTATION, OPACITY
 */
class PerformEngine {

    private val tracks = mutableMapOf<AnimProperty, PropertyTrack>().apply {
        AnimProperty.entries.forEach { put(it, PropertyTrack(it)) }
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _posX = MutableStateFlow(0f)
    private val _posY = MutableStateFlow(0f)
    private val _scale = MutableStateFlow(1f)
    private val _rotation = MutableStateFlow(0f)
    private val _opacity = MutableStateFlow(1f)

    val posX: StateFlow<Float> = _posX.asStateFlow()
    val posY: StateFlow<Float> = _posY.asStateFlow()
    val scale: StateFlow<Float> = _scale.asStateFlow()
    val rotation: StateFlow<Float> = _rotation.asStateFlow()
    val opacity: StateFlow<Float> = _opacity.asStateFlow()

    fun startRecording() { _isRecording.value = true }
    fun stopRecording() { _isRecording.value = false }
    fun toggleRecording() { _isRecording.value = !_isRecording.value }

    fun recordAtFrame(frame: Int, property: AnimProperty, value: Float, easing: EasingType = EasingType.EASE_IN_OUT) {
        if (!_isRecording.value) return
        val track = tracks[property] ?: return
        track.keyframes.removeAll { it.frame == frame }
        track.keyframes.add(Keyframe(frame, value, easing))
        applyLive(property, value)
    }

    fun recordDrag(frame: Int, dx: Float, dy: Float) {
        if (!_isRecording.value) return
        val nx = _posX.value + dx
        val ny = _posY.value + dy
        recordAtFrame(frame, AnimProperty.POS_X, nx)
        recordAtFrame(frame, AnimProperty.POS_Y, ny)
    }

    fun addKeyframe(property: AnimProperty, frame: Int, value: Float, easing: EasingType = EasingType.BEZIER) {
        val track = tracks[property] ?: return
        track.keyframes.removeAll { it.frame == frame }
        track.keyframes.add(Keyframe(frame, value, easing))
    }

    fun evaluate(frame: Float) {
        _posX.value = KeyframeInterpolator.interpolate(tracks[AnimProperty.POS_X]!!, frame, 0f)
        _posY.value = KeyframeInterpolator.interpolate(tracks[AnimProperty.POS_Y]!!, frame, 0f)
        _scale.value = KeyframeInterpolator.interpolate(tracks[AnimProperty.SCALE]!!, frame, 1f)
        _rotation.value = KeyframeInterpolator.interpolate(tracks[AnimProperty.ROTATION]!!, frame, 0f)
        _opacity.value = KeyframeInterpolator.interpolate(tracks[AnimProperty.OPACITY]!!, frame, 1f)
    }

    private fun applyLive(property: AnimProperty, value: Float) {
        when (property) {
            AnimProperty.POS_X -> _posX.value = value
            AnimProperty.POS_Y -> _posY.value = value
            AnimProperty.SCALE -> _scale.value = value
            AnimProperty.ROTATION -> _rotation.value = value
            AnimProperty.OPACITY -> _opacity.value = value
        }
    }

    fun getTrack(property: AnimProperty): PropertyTrack = tracks[property]!!

    fun clearAll() {
        tracks.values.forEach { it.keyframes.clear() }
        _posX.value = 0f
        _posY.value = 0f
        _scale.value = 1f
        _rotation.value = 0f
        _opacity.value = 1f
    }
}
