package com.proanimator.core.timeline

import com.proanimator.domain.model.Project
import com.proanimator.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimelineMode {
    COMPOSE,
    KEYFRAME,
    PERFORM
}

class TimelineEngine(
    initialProject: Project
) {
    private val _project = MutableStateFlow(initialProject)
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _currentFrame = MutableStateFlow(0)
    val currentFrame: StateFlow<Int> = _currentFrame.asStateFlow()

    private val _mode = MutableStateFlow(TimelineMode.COMPOSE)
    val mode: StateFlow<TimelineMode> = _mode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun setMode(mode: TimelineMode) {
        _mode.value = mode
    }

    fun seekTo(frame: Int) {
        _currentFrame.value = frame.coerceAtLeast(0)
    }

    fun play() {
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun addTrack(name: String = "Track ${_project.value.tracks.size + 1}") {
        val newTrack = Track(name = name)
        _project.value = _project.value.copy(
            tracks = _project.value.tracks + newTrack
        )
    }
}
