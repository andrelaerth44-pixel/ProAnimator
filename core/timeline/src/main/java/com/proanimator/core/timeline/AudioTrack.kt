package com.proanimator.core.timeline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Audio track model for timeline sync.
 * Playback wiring (MediaPlayer/ExoPlayer) is app-layer; this holds metadata.
 */
data class AudioClip(
    val id: String,
    val displayName: String,
    val uriString: String,
    val startFrame: Int = 0,
    val durationFrames: Int = 0,
    val volume: Float = 1f
)

class AudioTrackEngine {

    private val _clips = MutableStateFlow<List<AudioClip>>(emptyList())
    val clips: StateFlow<List<AudioClip>> = _clips.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    fun addClip(clip: AudioClip) {
        _clips.value = _clips.value + clip
    }

    fun removeClip(id: String) {
        _clips.value = _clips.value.filter { it.id != id }
    }

    fun setMuted(m: Boolean) { _muted.value = m }
    fun toggleMute() { _muted.value = !_muted.value }

    fun clear() { _clips.value = emptyList() }
}
