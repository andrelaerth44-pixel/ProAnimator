package com.proanimator.core.engine

import android.graphics.Bitmap
import android.graphics.PorterDuff
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.IntSize
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * LayerBitmapEngine
 *
 * Each layer owns a real ImageBitmap.
 * Drawing and erasing happen directly on the bitmap.
 * Eraser uses BlendMode.Clear for true pixel deletion.
 *
 * Based on research:
 * - SmartToolFactory Compose Drawing patterns
 * - Android BlendMode.Clear + offscreen compositing
 * - androidx.compose.ui.graphics.Canvas(ImageBitmap)
 */
class LayerBitmapEngine(
    val width: Int,
    val height: Int
) {
    data class LayerState(
        val id: String,
        val name: String,
        val bitmap: ImageBitmap,
        val isVisible: Boolean = true,
        val opacity: Float = 1f
    )

    private val _layers = MutableStateFlow<List<LayerState>>(emptyList())
    val layers: StateFlow<List<LayerState>> = _layers.asStateFlow()

    private val _activeLayerId = MutableStateFlow("")
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    // Live stroke being drawn (preview)
    private val _currentPath = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentPath: StateFlow<List<StrokePoint>> = _currentPath.asStateFlow()

    private val _toolMode = MutableStateFlow(ToolMode.DRAW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

    private val _brushSize = MutableStateFlow(12f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    private val _brushColor = MutableStateFlow(0xFFFFFFFF)
    val brushColor: StateFlow<Long> = _brushColor.asStateFlow()

    // Undo stack of layer bitmaps (simple snapshot approach for Phase 3)
    private val undoStack = mutableListOf<Map<String, Bitmap>>()
    private val redoStack = mutableListOf<Map<String, Bitmap>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        addLayer("Layer 1")
    }

    private fun createEmptyBitmap(): ImageBitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    fun addLayer(name: String = "Layer ${_layers.value.size + 1}") {
        val id = java.util.UUID.randomUUID().toString()
        val bitmap = createEmptyBitmap()
        _layers.update { it + LayerState(id, name, bitmap) }
        _activeLayerId.value = id
    }

    fun setActiveLayer(id: String) {
        if (_layers.value.any { it.id == id }) {
            _activeLayerId.value = id
        }
    }

    fun setToolMode(mode: ToolMode) {
        _toolMode.value = mode
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size.coerceIn(1f, 200f)
    }

    fun setBrushColor(color: Long) {
        _brushColor.value = color
        _toolMode.value = ToolMode.DRAW
    }

    fun toggleLayerVisibility(id: String) {
        _layers.update { list ->
            list.map { if (it.id == id) it.copy(isVisible = !it.isVisible) else it }
        }
    }

    fun startStroke(point: StrokePoint) {
        _currentPath.value = listOf(point)
    }

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

        pushUndoSnapshot()

        val layerId = _activeLayerId.value
        val layer = _layers.value.find { it.id == layerId } ?: return

        // Draw directly onto the layer's ImageBitmap
        val canvas = Canvas(layer.bitmap)

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        val paint = Paint().apply {
            strokeWidth = _brushSize.value
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            style = PaintingStyle.Stroke
            isAntiAlias = true
        }

        if (_toolMode.value == ToolMode.ERASE) {
            // True eraser: BlendMode.Clear removes pixels
            paint.blendMode = BlendMode.Clear
            paint.color = Color.Transparent
        } else {
            paint.blendMode = BlendMode.SrcOver
            paint.color = Color(_brushColor.value)
        }

        // Use saveLayer for correct blend isolation (research-backed)
        canvas.nativeCanvas.saveLayer(null, null)
        canvas.drawPath(path, paint)
        canvas.nativeCanvas.restore()

        // Force recomposition by creating a new list reference
        _layers.update { list ->
            list.map { if (it.id == layerId) it.copy(bitmap = layer.bitmap) else it }
        }

        _currentPath.value = emptyList()
        redoStack.clear()
        updateUndoRedoState()
    }

    private fun pushUndoSnapshot() {
        val snapshot = _layers.value.associate { layer ->
            layer.id to layer.bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        }
        undoStack.add(snapshot)
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        // Save current for redo
        val current = _layers.value.associate { layer ->
            layer.id to layer.bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        }
        redoStack.add(current)

        val previous = undoStack.removeAt(undoStack.lastIndex)
        restoreSnapshot(previous)
        updateUndoRedoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val current = _layers.value.associate { layer ->
            layer.id to layer.bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        }
        undoStack.add(current)

        val next = redoStack.removeAt(redoStack.lastIndex)
        restoreSnapshot(next)
        updateUndoRedoState()
    }

    private fun restoreSnapshot(snapshot: Map<String, Bitmap>) {
        _layers.update { list ->
            list.map { layer ->
                val bmp = snapshot[layer.id]
                if (bmp != null) {
                    layer.copy(bitmap = bmp.asImageBitmap())
                } else layer
            }
        }
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun clearActiveLayer() {
        pushUndoSnapshot()
        val layerId = _activeLayerId.value
        _layers.update { list ->
            list.map { layer ->
                if (layer.id == layerId) {
                    // Clear bitmap
                    val canvas = Canvas(layer.bitmap)
                    canvas.nativeCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    layer.copy(bitmap = layer.bitmap)
                } else layer
            }
        }
        redoStack.clear()
        updateUndoRedoState()
    }

    /** Returns all visible layer bitmaps for compositing / export */
    fun getVisibleBitmaps(): List<Pair<ImageBitmap, Float>> {
        return _layers.value
            .filter { it.isVisible }
            .map { it.bitmap to it.opacity }
    }
}
