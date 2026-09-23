package com.proanimator.core.engine

import com.proanimator.domain.model.Layer
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

class CanvasEngine(
    val width: Int,
    val height: Int
) {
    private val _layers = MutableStateFlow<List<Layer>>(listOf(Layer(name = "Layer 1")))
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()

    private val _activeLayerId = MutableStateFlow(_layers.value.first().id)
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    private val _strokesByLayer = MutableStateFlow<Map<String, List<Stroke>>>(emptyMap())
    val strokesByLayer: StateFlow<Map<String, List<Stroke>>> = _strokesByLayer.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyMap())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    private val _currentBrushId = MutableStateFlow("technical_pen")
    val currentBrushId: StateFlow<String> = _currentBrushId.asStateFlow()

    private val _currentColor = MutableStateFlow(0xFFFFFFFF)
    val currentColor: StateFlow<Long> = _currentColor.asStateFlow()

    private val _currentSize = MutableStateFlow(8f)
    val currentSize: StateFlow<Float> = _currentSize.asStateFlow()

    private val _toolMode = MutableStateFlow(ToolMode.DRAW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

    private val _stabilization = MutableStateFlow(0.35f)
    val stabilization: StateFlow<Float> = _stabilization.asStateFlow()

    private val undoStack = mutableListOf<Map<String, List<Stroke>>>()
    private val redoStack = mutableListOf<Map<String, List<Stroke>>>()

    fun setActiveLayer(id: String) {
        if (_layers.value.any { it.id == id }) _activeLayerId.value = id
    }

    fun setToolMode(mode: ToolMode) {
        _toolMode.value = mode
    }

    fun setBrush(id: String) {
        _currentBrushId.value = id
        _toolMode.value = ToolMode.DRAW
    }

    fun setColor(color: Long) {
        _currentColor.value = color
        _toolMode.value = ToolMode.DRAW
    }

    fun setSize(size: Float) {
        _currentSize.value = size.coerceIn(0.5f, 256f)
    }

    fun setStabilization(v: Float) {
        _stabilization.value = v.coerceIn(0f, 1f)
    }

    fun beginStroke(point: StrokePoint) {
        pushUndo()
        _currentStroke.value = listOf(point)
    }

    fun appendStroke(point: StrokePoint) {
        val cur = _currentStroke.value
        if (cur.isEmpty()) return
        val last = cur.last()
        val dx = point.x - last.x
        val dy = point.y - last.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.5f) return
        val s = _stabilization.value
        val smoothed = if (s <= 0f) point else point.copy(
            x = last.x + (point.x - last.x) * (1f - s * 0.85f),
            y = last.y + (point.y - last.y) * (1f - s * 0.85f)
        )
        _currentStroke.value = cur + smoothed
    }

    fun endStroke() {
        val pts = _currentStroke.value
        if (pts.size < 2) {
            _currentStroke.value = emptyList()
            return
        }
        val layerId = _activeLayerId.value
        val isEraser = _toolMode.value == ToolMode.ERASE
        val stroke = Stroke(
            points = pts,
            color = if (isEraser) 0x00000000 else _currentColor.value,
            size = _currentSize.value,
            brushId = if (isEraser) "eraser" else _currentBrushId.value,
            isEraser = isEraser
        )
        _strokesByLayer.update { map ->
            val list = map[layerId].orEmpty() + stroke
            map + (layerId to list)
        }
        _currentStroke.value = emptyList()
    }

    fun cancelStroke() {
        _currentStroke.value = emptyList()
    }

    fun clearActiveLayer() {
        pushUndo()
        val id = _activeLayerId.value
        _strokesByLayer.update { it + (id to emptyList()) }
    }

    fun addLayer(name: String = "Layer ${_layers.value.size + 1}") {
        val layer = Layer(name = name)
        _layers.update { it + layer }
        _activeLayerId.value = layer.id
    }

    fun removeLayer(id: String) {
        if (_layers.value.size <= 1) return
        pushUndo()
        _layers.update { it.filter { l -> l.id != id } }
        _strokesByLayer.update { it - id }
        if (_activeLayerId.value == id) {
            _activeLayerId.value = _layers.value.first().id
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(_strokesByLayer.value)
        _strokesByLayer.value = undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_strokesByLayer.value)
        _strokesByLayer.value = redoStack.removeAt(redoStack.lastIndex)
    }

    private fun pushUndo() {
        undoStack.add(_strokesByLayer.value)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun getSerializableState(): Map<String, Any> {
        return mapOf(
            "width" to width,
            "height" to height,
            "layers" to _layers.value,
            "strokes" to _strokesByLayer.value
        )
    }
}
