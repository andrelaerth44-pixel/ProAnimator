package com.proanimator.core.timeline

import android.graphics.Bitmap
import android.graphics.PorterDuff
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import com.proanimator.core.brushes.BrushEngine
import com.proanimator.core.brushes.BrushLibrary
import com.proanimator.core.brushes.BrushPreset
import com.proanimator.core.engine.ToolMode
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class FlipbookBitmapEngine(
    val width: Int = 1920,
    val height: Int = 1080
) {
    data class Frame(
        val id: String = UUID.randomUUID().toString(),
        val index: Int,
        val layers: LayerStack
    ) {
        val bitmap: ImageBitmap get() = layers.composite()
    }

    val brushEngine = BrushEngine()

    private val _frames = MutableStateFlow(
        listOf(Frame(index = 0, layers = LayerStack(width, height)))
    )
    val frames: StateFlow<List<Frame>> = _frames.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val currentFrame: Frame?
        get() = _frames.value.getOrNull(_currentIndex.value)

    private val _layerRevision = MutableStateFlow(0)
    val layerRevision: StateFlow<Int> = _layerRevision.asStateFlow()

    private val _currentPath = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentPath: StateFlow<List<StrokePoint>> = _currentPath.asStateFlow()

    private val _toolMode = MutableStateFlow(ToolMode.DRAW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

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

    fun bumpLayers() { _layerRevision.value++ }

    fun setToolMode(mode: ToolMode) {
        _toolMode.value = mode
        if (mode == ToolMode.ERASE) brushEngine.setBrush(BrushLibrary.ERASER)
        else if (brushEngine.activeBrush.value.isEraser) brushEngine.setBrush(BrushLibrary.PEN)
    }

    fun setBrush(preset: BrushPreset) {
        brushEngine.setBrush(preset)
        _toolMode.value = if (preset.isEraser) ToolMode.ERASE else ToolMode.DRAW
    }

    fun setBrushColor(color: Long) {
        brushEngine.setColor(color)
        if (brushEngine.activeBrush.value.isEraser) {
            brushEngine.setBrush(BrushLibrary.PEN)
            _toolMode.value = ToolMode.DRAW
        }
    }

    fun setSizeMultiplier(m: Float) = brushEngine.setSizeMultiplier(m)
    fun setOnionEnabled(enabled: Boolean) { _onionEnabled.value = enabled }

    fun setCurrentFrame(index: Int) {
        val max = (_frames.value.size - 1).coerceAtLeast(0)
        _currentIndex.value = index.coerceIn(0, max)
        _currentPath.value = emptyList()
        bumpLayers()
    }

    fun nextFrame() {
        val next = _currentIndex.value + 1
        if (next >= _frames.value.size) {
            addFrame()
            _currentIndex.value = _frames.value.size - 1
        } else _currentIndex.value = next
        _currentPath.value = emptyList()
        bumpLayers()
    }

    fun previousFrame() { setCurrentFrame(_currentIndex.value - 1) }

    fun addFrame() {
        val newIndex = _frames.value.size
        _frames.update { it + Frame(index = newIndex, layers = LayerStack(width, height)) }
    }

    fun duplicateCurrentFrame() {
        val current = currentFrame ?: return
        val stack = LayerStack(width, height)
        stack.duplicateFrom(current.layers)
        val newIndex = _frames.value.size
        _frames.update { it + Frame(index = newIndex, layers = stack) }
        _currentIndex.value = newIndex
        bumpLayers()
    }

    fun deleteCurrentFrame() {
        if (_frames.value.size <= 1) return
        val idx = _currentIndex.value
        _frames.update { list ->
            list.filterIndexed { i, _ -> i != idx }.mapIndexed { i, f -> f.copy(index = i) }
        }
        if (_currentIndex.value >= _frames.value.size) {
            _currentIndex.value = _frames.value.size - 1
        }
        bumpLayers()
    }

    fun addLayer() {
        currentFrame?.layers?.addLayer()
        bumpLayers()
    }

    fun removeActiveLayer() {
        currentFrame?.layers?.let { stack ->
            stack.removeLayer(stack.activeLayerIndex)
            bumpLayers()
        }
    }

    fun setActiveLayer(index: Int) {
        currentFrame?.layers?.setActive(index)
        bumpLayers()
    }

    fun toggleLayerVisibility(index: Int) {
        currentFrame?.layers?.toggleVisibility(index)
        bumpLayers()
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        currentFrame?.layers?.setLayerOpacity(index, opacity)
        bumpLayers()
    }

    fun setLayerBlendMode(index: Int, mode: LayerBlendMode) {
        currentFrame?.layers?.setLayerBlendMode(index, mode)
        bumpLayers()
    }

    fun loadFrames(bitmaps: List<ImageBitmap>, startIndex: Int = 0) {
        if (bitmaps.isEmpty()) return
        undoStack.clear()
        redoStack.clear()
        updateUndoRedo()
        _frames.value = bitmaps.mapIndexed { i, bmp ->
            val stack = LayerStack(width, height)
            stack.replaceActiveBitmap(bmp)
            Frame(index = i, layers = stack)
        }
        _currentIndex.value = startIndex.coerceIn(0, bitmaps.size - 1)
        _currentPath.value = emptyList()
        bumpLayers()
    }

    fun loadLayerStacks(stacks: List<LayerStack>, startIndex: Int = 0) {
        if (stacks.isEmpty()) return
        undoStack.clear()
        redoStack.clear()
        updateUndoRedo()
        _frames.value = stacks.mapIndexed { i, src ->
            val stack = LayerStack(width, height, initialLayers = 0)
            stack.replaceAll(src)
            Frame(index = i, layers = stack)
        }
        _currentIndex.value = startIndex.coerceIn(0, stacks.size - 1)
        _currentPath.value = emptyList()
        bumpLayers()
    }

    fun startStroke(point: StrokePoint) { _currentPath.value = listOf(point) }

    fun addPoint(point: StrokePoint) {
        _currentPath.update { current ->
            if (current.isEmpty()) listOf(point)
            else {
                val last = current.last()
                val dx = point.x - last.x
                val dy = point.y - last.y
                if (dx * dx + dy * dy > 1.2f) current + point else current
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
        val active = frame.layers.activeLayer()
        pushUndo(frame.index, active.bitmap)
        brushEngine.drawStroke(active.bitmap, points)
        _currentPath.value = emptyList()
        redoStack.clear()
        updateUndoRedo()
        bumpLayers()
    }

    fun cancelStroke() { _currentPath.value = emptyList() }

    private fun pushUndo(frameIndex: Int, bitmap: ImageBitmap) {
        val copy = bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        undoStack.add(frameIndex to copy)
        if (undoStack.size > 40) undoStack.removeAt(0)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val (frameIndex, bmp) = undoStack.removeAt(undoStack.lastIndex)
        val frame = _frames.value.getOrNull(frameIndex) ?: return
        val current = frame.layers.activeLayer().bitmap
        redoStack.add(frameIndex to current.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true))
        frame.layers.restoreActive(bmp)
        updateUndoRedo()
        bumpLayers()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val (frameIndex, bmp) = redoStack.removeAt(redoStack.lastIndex)
        val frame = _frames.value.getOrNull(frameIndex) ?: return
        val current = frame.layers.activeLayer().bitmap
        undoStack.add(frameIndex to current.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true))
        frame.layers.restoreActive(bmp)
        updateUndoRedo()
        bumpLayers()
    }

    private fun updateUndoRedo() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun clearCurrentFrame() {
        val frame = currentFrame ?: return
        val active = frame.layers.activeLayer()
        pushUndo(frame.index, active.bitmap)
        val canvas = Canvas(active.bitmap)
        canvas.nativeCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        redoStack.clear()
        updateUndoRedo()
        bumpLayers()
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
