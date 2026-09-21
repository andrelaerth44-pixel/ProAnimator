package com.proanimator.core.timeline

import com.proanimator.domain.model.AnimatableProperty
import com.proanimator.domain.model.EasingType
import com.proanimator.domain.model.Keyframe
import com.proanimator.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

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

    // Simple property animation store (for demo / foundation)
    // In real use this will be attached to Content items
    private val _properties = MutableStateFlow<Map<String, AnimatableProperty>>(emptyMap())
    val properties: StateFlow<Map<String, AnimatableProperty>> = _properties.asStateFlow()

    fun setMode(mode: TimelineMode) {
        _mode.value = mode
    }

    fun setFps(value: Float) {
        _fps.value = value.coerceIn(1f, 60f)
    }

    fun setDuration(frames: Int) {
        _durationFrames.value = frames.coerceAtLeast(24)
    }

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

    fun removeTrack(trackId: String) {
        if (_tracks.value.size <= 1) return
        _tracks.update { it.filter { it.id != trackId } }
    }

    // ===== Keyframe API =====

    fun addOrUpdateKeyframe(
        propertyName: String,
        frame: Int,
        value: Float,
        easing: EasingType = EasingType.EASE_IN_OUT
    ) {
        _properties.update { current ->
            val existing = current[propertyName] ?: AnimatableProperty(propertyName)
            val filtered = existing.keyframes.filter { it.frame != frame }
            val newKeyframes = (filtered + Keyframe(frame = frame, value = value, easing = easing))
                .sortedBy { it.frame }

            current + (propertyName to existing.copy(keyframes = newKeyframes))
        }
    }

    fun removeKeyframe(propertyName: String, frame: Int) {
        _properties.update { current ->
            val existing = current[propertyName] ?: return@update current
            val newKeyframes = existing.keyframes.filter { it.frame != frame }
            current + (propertyName to existing.copy(keyframes = newKeyframes))
        }
    }

    fun getPropertyValue(propertyName: String, frame: Int = _currentFrame.value): Float {
        return _properties.value[propertyName]?.valueAt(frame) ?: 0f
    }

    fun getKeyframes(propertyName: String): List<Keyframe> {
        return _properties.value[propertyName]?.keyframes ?: emptyList()
    }

    fun clearProperty(propertyName: String) {
        _properties.update { it - propertyName }
    }
}
