package com.proanimator.app.ui.workspace

import android.net.Uri
import com.proanimator.core.brushes.BrushLibrary
import com.proanimator.core.engine.CanvasViewport
import com.proanimator.core.export.ProjectSerializer
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.PerformEngine
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode
import androidx.compose.ui.geometry.Offset

object WorkspaceScreenHooks {

    data class ViewportState(val scale: Float, val offset: Offset)

    suspend fun loadPanUri(
        serializer: ProjectSerializer,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        timeline: TimelineEngine,
        uri: Uri,
        viewport: CanvasViewport? = null,
        viewW: Float = 0f,
        viewH: Float = 0f
    ): Pair<String, ViewportState?> {
        val data = serializer.loadFromUri(uri).getOrElse {
            return "PAN fail: ${it.message?.take(32)}" to null
        }
        applyProject(data, flipbook, perform, timeline)
        val vp = fitIfPossible(viewport, viewW, viewH)
        val msg = "Loaded ${data.frames.size}f / ${perform.keyframeCount()} KF" +
            if (data.layerStacks.isNotEmpty()) " (layers)" else "" +
            if (vp != null) " · fit" else ""
        return msg to vp
    }

    suspend fun loadPan(
        serializer: ProjectSerializer,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        timeline: TimelineEngine,
        file: java.io.File,
        viewport: CanvasViewport? = null,
        viewW: Float = 0f,
        viewH: Float = 0f
    ): Pair<String, ViewportState?> {
        val data = serializer.load(file).getOrElse {
            return "PAN fail: ${it.message?.take(32)}" to null
        }
        applyProject(data, flipbook, perform, timeline)
        val vp = fitIfPossible(viewport, viewW, viewH)
        return "Loaded ${data.frames.size}f · fit" to vp
    }

    private fun fitIfPossible(
        viewport: CanvasViewport?,
        viewW: Float,
        viewH: Float
    ): ViewportState? {
        if (viewport == null || viewW <= 0f || viewH <= 0f) return null
        viewport.fitToScreen(viewW, viewH)
        return ViewportState(viewport.scale, viewport.offset)
    }

    private fun applyProject(
        data: ProjectSerializer.ProjectData,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        timeline: TimelineEngine
    ) {
        if (data.layerStacks.isNotEmpty()) {
            flipbook.loadLayerStacks(data.layerStacks, data.currentFrameIndex)
        } else {
            flipbook.loadFrames(data.frames, data.currentFrameIndex)
        }
        val extras = ProjectSerializer.applyMeta(data.meta, perform)
        flipbook.setOnionEnabled(extras.onionEnabled)
        BrushLibrary.ALL.find { it.id == extras.brushId }?.let { flipbook.setBrush(it) }
        try {
            timeline.setMode(TimelineMode.valueOf(extras.timelineMode))
        } catch (_: Exception) {
        }
        perform.evaluate(data.currentFrameIndex.toFloat())
    }

    suspend fun saveInternal(
        serializer: ProjectSerializer,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        fps: Float,
        brushId: String,
        onion: Boolean,
        mode: String
    ): String {
        val meta = ProjectSerializer.buildMeta(brushId, onion, mode, perform)
        return serializer.saveFromFlipbook(flipbook, fps, meta).fold(
            onSuccess = { "Saved ${it.name} (PAN2)" },
            onFailure = { "Save fail: ${it.message?.take(32)}" }
        )
    }

    suspend fun saveToUri(
        serializer: ProjectSerializer,
        uri: Uri,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        fps: Float,
        brushId: String,
        onion: Boolean,
        mode: String
    ): String {
        val meta = ProjectSerializer.buildMeta(brushId, onion, mode, perform)
        return serializer.saveToUri(uri, flipbook, fps, meta).fold(
            onSuccess = { "Exported .pan" },
            onFailure = { "Export fail: ${it.message?.take(32)}" }
        )
    }
}
