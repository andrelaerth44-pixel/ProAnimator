package com.proanimator.core.timeline

import com.proanimator.domain.model.AnimatableProperty
import com.proanimator.domain.model.EasingType
import com.proanimator.domain.model.Keyframe
import com.proanimator.domain.model.PropertyType
import com.proanimator.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TimelineMode {
    COMPOSE,
    KEYFRAME,
    PERFORM
}

class TimelineEngine(
    initialFps: Float = 24f,
    initialDurationFrames: Int = 120
) {
    private val _fps = MutableStateFlow(initialFps)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _durationFrames = MutableStateFlow(initialDurationFrames)
    val durationFrames: StateFlow<Int> = _durationFrames.asStateFlow()

    private val _currentFrame = MutableStateFlow(0)
    val currentFrame: StateFlow<Int> = _currentFrame.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _mode = MutableStateFlow(TimelineMode.COMPOSE)
    val mode: StateFlow<TimelineMode> = _mode.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(listOf(Track(name = "Track 1")))
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    // Multiple animatable properties
    private val _properties = MutableStateFlow<Map<PropertyType, AnimatableProperty>>(emptyMap())
    val properties: StateFlow<Map<PropertyType, AnimatableProperty>> = _properties.asStateFlow()

    // Perform mode recording
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun setMode(mode: TimelineMode) {
        _mode.value = mode
        if (mode != TimelineMode.PERFORM) {
            _isRecording.value = false
        }
    }

    fun setFps(value: Float) { _fps.value = value.coerceIn(1f, 60f) }
    fun setDuration(frames: Int) { _durationFrames.value = frames.coerceAtLeast(24) }

    fun seekTo(frame: Int) {
        _currentFrame.value = frame.coerceIn(0, _durationFrames.value)
    }

    fun play() { _isPlaying.value = true }
    fun pause() { _isPlaying.value = false }
    fun togglePlay() { if (_isPlaying.value) pause() else play() }

    fun nextFrame() = seekTo(_currentFrame.value + 1)
    fun previousFrame() = seekTo(_currentFrame.value - 1)

    fun tick() {
        if (!_isPlaying.value) return
        val next = _currentFrame.value + 1
        if (next > _durationFrames.value) seekTo(0) else seekTo(next)
    }

    fun addTrack(name: String = "Track ${_tracks.value.size + 1}") {
        _tracks.update { it + Track(name = name) }
    }

    // ===== Keyframe API (expanded) =====

    fun addOrUpdateKeyframe(
        type: PropertyType,
        frame: Int,
        value: Float,
        easing: EasingType = EasingType.EASE_IN_OUT
    ) {
        _properties.update { current ->
            val existing = current[type] ?: AnimatableProperty(type)
            val filtered = existing.keyframes.filter { it.frame != frame }
            val newKeyframes = (filtered + Keyframe(frame = frame, value = value, easing = easing))
                .sortedBy { it.frame }
            current + (type to existing.copy(keyframes = newKeyframes))
        }
    }

    fun getPropertyValue(type: PropertyType, frame: Int = _currentFrame.value): Float {
        return _properties.value[type]?.valueAt(frame) ?: when (type) {
            PropertyType.OPACITY -> 1f
            PropertyType.SCALE -> 1f
            else -> 0f
        }
    }

    fun getKeyframes(type: PropertyType): List<Keyframe> {
        return _properties.value[type]?.keyframes ?: emptyList()
    }

    // ===== Perform mode =====

    fun startRecording() {
        if (_mode.value == TimelineMode.PERFORM) {
            _isRecording.value = true
            play()
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        pause()
    }

    fun toggleRecording() {
        if (_isRecording.value) stopRecording() else startRecording()
    }

    /** Called during Perform mode when user moves something */
    fun recordProperty(type: PropertyType, value: Float) {
        if (!_isRecording.value) return
        addOrUpdateKeyframe(type, _currentFrame.value, value, EasingType.LINEAR)
    }
}
