package com.proanimator.core.timeline

import android.graphics.Bitmap
import android.graphics.PorterDuff
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import com.proanimator.core.engine.ToolMode
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * FlipbookBitmapEngine
 *
 * Each Flipbook frame owns a real ImageBitmap.
 * Drawing and erasing happen on the current frame's bitmap.
 * Onion skin reads previous/next frame bitmaps.
 * Supports full load of ProjectData frames.
 */
class FlipbookBitmapEngine(
    val width: Int = 1920,
    val height: Int = 1080
) {
    data class Frame(
        val id: String = UUID.randomUUID().toString(),
        val index: Int,
        val bitmap: ImageBitmap
    )

    private val _frames = MutableStateFlow<List<Frame>>(
        listOf(Frame(index = 0, bitmap = createEmptyBitmap()))
    )
    val frames: StateFlow<List<Frame>> = _frames.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val currentFrame: Frame?
        get() = _frames.value.getOrNull(_currentIndex.value)

    private val _currentPath = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentPath: StateFlow<List<StrokePoint>> = _currentPath.asStateFlow()

    private val _toolMode = MutableStateFlow(ToolMode.DRAW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

    private val _brushSize = MutableStateFlow(12f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    private val _brushColor = MutableStateFlow(0xFFFFFFFF)
    val brushColor: StateFlow<Long> = _brushColor.asStateFlow()

    private val _onionEnabled = MutableStateFlow(true)
    val onionEnabled: StateFlow<Boolean> = _onionEnabled.asStateFlow()

    private val _onionBefore = MutableStateFlow(2)
    private val _onionAfter = MutableStateFlow(1)

    private val undoStack = mutableListOf<Pair<Int, Bitmap>>()
    private val redoStack = mutableListOf<Pair<Int, Bitmap>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun createEmptyBitmap(): ImageBitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    fun setToolMode(mode: ToolMode) { _toolMode.value = mode }
    fun setBrushSize(size: Float) { _brushSize.value = size.coerceIn(1f, 200f) }
    fun setBrushColor(color: Long) {
        _brushColor.value = color
        _toolMode.value = ToolMode.DRAW
    }
    fun setOnionEnabled(enabled: Boolean) { _onionEnabled.value = enabled }

    fun setCurrentFrame(index: Int) {
        val max = (_frames.value.size - 1).coerceAtLeast(0)
        _currentIndex.value = index.coerceIn(0, max)
        _currentPath.value = emptyList()
    }

    fun nextFrame() {
        val next = _currentIndex.value + 1
        if (next >= _frames.value.size) {
            addFrame()
            _currentIndex.value = _frames.value.size - 1
        } else {
            _currentIndex.value = next
        }
        _currentPath.value = emptyList()
    }

    fun previousFrame() { setCurrentFrame(_currentIndex.value - 1) }

    fun addFrame() {
        val newIndex = _frames.value.size
        _frames.update { it + Frame(index = newIndex, bitmap = createEmptyBitmap()) }
    }

    fun duplicateCurrentFrame() {
        val current = currentFrame ?: return
        val copy = current.bitmap.asAndroidBitmap()
            .copy(Bitmap.Config.ARGB_8888, true)
            .asImageBitmap()
        val newIndex = _frames.value.size
        _frames.update { it + Frame(index = newIndex, bitmap = copy) }
        _currentIndex.value = newIndex
    }

    fun deleteCurrentFrame() {
        if (_frames.value.size <= 1) return
        val idx = _currentIndex.value
        _frames.update { list ->
            list.filterIndexed { i, _ -> i != idx }
                .mapIndexed { i, f -> f.copy(index = i) }
        }
        if (_currentIndex.value >= _frames.value.size) {
            _currentIndex.value = _frames.value.size - 1
        }
    }

    /** Load a full project (from .pan) */
    fun loadFrames(bitmaps: List<ImageBitmap>, startIndex: Int = 0) {
        if (bitmaps.isEmpty()) return
        undoStack.clear()
        redoStack.clear()
        updateUndoRedo()
        _frames.value = bitmaps.mapIndexed { i, bmp ->
            Frame(index = i, bitmap = bmp)
        }
        _currentIndex.value = startIndex.coerceIn(0, bitmaps.size - 1)
        _currentPath.value = emptyList()
    }

    fun startStroke(point: StrokePoint) { _currentPath.value = listOf(point) }

    fun addPoint(point: StrokePoint) {
        _currentPath.update { current ->
            if (current.isEmpty()) listOf(point)
            else {
                val last = current.last()
                val dx = point.x - last.x
                val dy = point.y - last.y
                if (dx * dx + dy * dy > 1.5f) current + point else current
            }
        }
    }

    fun endStroke() {
        val points = _currentPath.value
        if (points.size < 2) {
            _currentPath.value = emptyList()
            return
        }

        val frame = currentFrame ?: return
        pushUndo(frame.index, frame.bitmap)

        val canvas = Canvas(frame.bitmap)
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }

        val paint = Paint().apply {
            strokeWidth = _brushSize.value
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            style = PaintingStyle.Stroke
            isAntiAlias = true
        }

        if (_toolMode.value == ToolMode.ERASE) {
            paint.blendMode = BlendMode.Clear
            paint.color = Color.Transparent
        } else {
            paint.blendMode = BlendMode.SrcOver
            paint.color = Color(_brushColor.value)
        }

        canvas.nativeCanvas.saveLayer(null, null)
        canvas.drawPath(path, paint)
        canvas.nativeCanvas.restore()

        _frames.update { list ->
            list.map { if (it.id == frame.id) it.copy(bitmap = frame.bitmap) else it }
        }

        _currentPath.value = emptyList()
        redoStack.clear()
        updateUndoRedo()
    }

    private fun pushUndo(frameIndex: Int, bitmap: ImageBitmap) {
        val copy = bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        undoStack.add(frameIndex to copy)
        if (undoStack.size > 40) undoStack.removeAt(0)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val (frameIndex, bmp) = undoStack.removeAt(undoStack.lastIndex)
        val current = _frames.value.getOrNull(frameIndex)?.bitmap
        if (current != null) {
            redoStack.add(frameIndex to current.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true))
        }
        restoreFrameBitmap(frameIndex, bmp)
        updateUndoRedo()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val (frameIndex, bmp) = redoStack.removeAt(redoStack.lastIndex)
        val current = _frames.value.getOrNull(frameIndex)?.bitmap
        if (current != null) {
            undoStack.add(frameIndex to current.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true))
        }
        restoreFrameBitmap(frameIndex, bmp)
        updateUndoRedo()
    }

    private fun restoreFrameBitmap(frameIndex: Int, bmp: Bitmap) {
        _frames.update { list ->
            list.map { frame ->
                if (frame.index == frameIndex) frame.copy(bitmap = bmp.asImageBitmap()) else frame
            }
        }
    }

    private fun updateUndoRedo() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun clearCurrentFrame() {
        val frame = currentFrame ?: return
        pushUndo(frame.index, frame.bitmap)
        val canvas = Canvas(frame.bitmap)
        canvas.nativeCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        _frames.update { list ->
            list.map { if (it.id == frame.id) it.copy(bitmap = frame.bitmap) else it }
        }
        redoStack.clear()
        updateUndoRedo()
    }

    data class OnionLayer(val bitmap: ImageBitmap, val tint: Color, val alpha: Float)

    fun getOnionLayers(): List<OnionLayer> {
        if (!_onionEnabled.value) return emptyList()
        val result = mutableListOf<OnionLayer>()
        val current = _currentIndex.value
        val frames = _frames.value

        val prevColors = listOf(Color(0xFFFF5252), Color(0xFFFF8A65), Color(0xFFFFAB91))
        for (i in 1.._onionBefore.value) {
            val idx = current - i
            if (idx >= 0) {
                val alpha = (0.35f - (i - 1) * 0.1f).coerceIn(0.1f, 0.4f)
                result.add(OnionLayer(frames[idx].bitmap, prevColors.getOrElse(i - 1) { prevColors[0] }, alpha))
            }
        }

        val nextColors = listOf(Color(0xFF69F0AE), Color(0xFF00E676))
        for (i in 1.._onionAfter.value) {
            val idx = current + i
            if (idx < frames.size) {
                val alpha = (0.25f - (i - 1) * 0.08f).coerceIn(0.08f, 0.3f)
                result.add(OnionLayer(frames[idx].bitmap, nextColors.getOrElse(i - 1) { nextColors[0] }, alpha))
            }
        }
        return result
    }

    fun getAllBitmaps(): List<ImageBitmap> = _frames.value.map { it.bitmap }
}
