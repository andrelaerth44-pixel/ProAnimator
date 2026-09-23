package com.proanimator.app.ui.workspace

import android.content.Context
import android.net.Uri
import com.proanimator.core.brushes.BrushLibrary
import com.proanimator.core.export.ProjectSerializer
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.PerformEngine
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode

/**
 * Shared load/save logic so WorkspaceScreen stays thinner.
 */
object WorkspaceScreenHooks {

    suspend fun loadPan(
        serializer: ProjectSerializer,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        timeline: TimelineEngine,
        file: java.io.File
    ): String {
        val data = serializer.load(file).getOrElse {
            return "PAN fail: ${it.message?.take(32)}"
        }
        applyProject(data, flipbook, perform, timeline)
        return "Loaded ${data.frames.size}f / ${perform.keyframeCount()} KF" +
            if (data.layerStacks.isNotEmpty()) " (layers)" else ""
    }

    suspend fun loadPanUri(
        serializer: ProjectSerializer,
        flipbook: FlipbookBitmapEngine,
        perform: PerformEngine,
        timeline: TimelineEngine,
        uri: Uri
    ): String {
        val data = serializer.loadFromUri(uri).getOrElse {
            return "PAN fail: ${it.message?.take(32)}"
        }
        applyProject(data, flipbook, perform, timeline)
        return "Loaded ${data.frames.size}f / ${perform.keyframeCount()} KF" +
            if (data.layerStacks.isNotEmpty()) " (layers)" else ""
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
