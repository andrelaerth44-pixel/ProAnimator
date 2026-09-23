package com.proanimator.core.timeline

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Plays timeline audio clips with MediaPlayer.
 * Sync: seekTo frame via fps → ms.
 */
class AudioPlayer(
    private val context: Context,
    private val track: AudioTrackEngine
) {
    private var player: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasClip = MutableStateFlow(false)
    val hasClip: StateFlow<Boolean> = _hasClip.asStateFlow()

    private val _clipName = MutableStateFlow<String?>(null)
    val clipName: StateFlow<String?> = _clipName.asStateFlow()

    fun loadFromUri(uri: Uri, displayName: String = "Audio", fps: Float = 24f) {
        release()
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { /* may already have or not persistable */ }

        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(context, uri)
            prepare()
        }
        player = mp
        val durationMs = mp.duration.coerceAtLeast(0)
        val durationFrames = ((durationMs / 1000f) * fps).toInt().coerceAtLeast(1)

        track.clear()
        track.addClip(
            AudioClip(
                id = UUID.randomUUID().toString(),
                displayName = displayName,
                uriString = uri.toString(),
                startFrame = 0,
                durationFrames = durationFrames,
                volume = 1f
            )
        )
        _hasClip.value = true
        _clipName.value = displayName
    }

    fun play() {
        val p = player ?: return
        if (track.muted.value) return
        try {
            p.start()
            _isPlaying.value = true
        } catch (_: Exception) {
            _isPlaying.value = false
        }
    }

    fun pause() {
        try {
            player?.pause()
        } catch (_: Exception) {}
        _isPlaying.value = false
    }

    fun stop() {
        try {
            player?.pause()
            player?.seekTo(0)
        } catch (_: Exception) {}
        _isPlaying.value = false
    }

    /** Sync playhead: frame index → media position */
    fun seekToFrame(frame: Int, fps: Float) {
        val p = player ?: return
        val ms = ((frame / fps.coerceAtLeast(1f)) * 1000f).toInt().coerceIn(0, p.duration)
        try {
            p.seekTo(ms)
        } catch (_: Exception) {}
    }

    fun setVolume(v: Float) {
        val vol = v.coerceIn(0f, 1f)
        try {
            player?.setVolume(vol, vol)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            player?.release()
        } catch (_: Exception) {}
        player = null
        _isPlaying.value = false
        _hasClip.value = false
        _clipName.value = null
    }
}
