package com.proanimator.core.timeline

import com.proanimator.domain.model.Content
import com.proanimator.domain.model.Project
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
    initialDurationFrames: Int = 120 // 5 seconds at 24fps
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

    private val _tracks = MutableStateFlow<List<Track>>(
        listOf(Track(name = "Track 1"))
    )
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

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
        if (_isPlaying.value && frame >= _durationFrames.value) {
            pause()
        }
    }

    fun play() {
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun togglePlay() {
        if (_isPlaying.value) pause() else play()
    }

    fun nextFrame() {
        seekTo(_currentFrame.value + 1)
    }

    fun previousFrame() {
        seekTo(_currentFrame.value - 1)
    }

    fun addTrack(name: String = "Track ${_tracks.value.size + 1}") {
        _tracks.update { it + Track(name = name) }
    }

    fun removeTrack(trackId: String) {
        if (_tracks.value.size <= 1) return
        _tracks.update { it.filter { t -> t.id != trackId } }
    }

    fun renameTrack(trackId: String, newName: String) {
        _tracks.update { list ->
            list.map { if (it.id == trackId) it.copy(name = newName) else it }
        }
    }

    // Advance playhead (called by a ticker)
    fun tick() {
        if (!_isPlaying.value) return
        val next = _currentFrame.value + 1
        if (next > _durationFrames.value) {
            seekTo(0) // loop for now
        } else {
            seekTo(next)
        }
    }

    fun getTimeString(): String {
        val totalSeconds = _currentFrame.value / _fps.value
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val frames = _currentFrame.value % _fps.value.toInt()
        return "%02d:%02d:%02d".format(minutes, seconds, frames)
    }
}
