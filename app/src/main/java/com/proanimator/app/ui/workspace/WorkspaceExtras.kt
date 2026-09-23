package com.proanimator.app.ui.workspace

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.proanimator.core.engine.TransformTool
import com.proanimator.core.export.ExportEngine
import com.proanimator.core.export.ProjectSerializer
import com.proanimator.core.timeline.AudioPlayer
import com.proanimator.core.timeline.AudioTrackEngine
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.PerformEngine
import com.proanimator.core.timeline.TimelineEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Central wiring for Warp / Transform / Audio so WorkspaceScreen stays focused.
 * Call from WorkspaceScreen.
 */
class WorkspaceExtras(
    val context: Context,
    val scope: CoroutineScope,
    val flipbook: FlipbookBitmapEngine,
    val timeline: TimelineEngine,
    val perform: PerformEngine,
    val serializer: ProjectSerializer,
    val exportEngine: ExportEngine,
    val audioTrack: AudioTrackEngine = AudioTrackEngine(),
    val transformTool: TransformTool = TransformTool()
) {
    val audioPlayer = AudioPlayer(context, audioTrack)

    fun onAudioPicked(uri: Uri) {
        val name = uri.lastPathSegment ?: "audio"
        audioPlayer.loadFromUri(uri, name, timeline.fps.value)
    }

    fun syncAudioToPlayhead() {
        audioPlayer.seekToFrame(timeline.currentFrame.value, timeline.fps.value)
    }

    fun onTimelinePlayChanged(playing: Boolean) {
        if (playing) {
            audioPlayer.seekToFrame(timeline.currentFrame.value, timeline.fps.value)
            audioPlayer.play()
        } else {
            audioPlayer.pause()
        }
    }

    fun applyTransformAndClose() {
        val s = transformTool.state.value
        if (s.enabled && (s.tx != 0f || s.ty != 0f || s.scale != 1f || s.rotation != 0f)) {
            flipbook.bakeTransform(s.tx, s.ty, s.scale, s.rotation)
        }
        transformTool.disable()
    }
}
