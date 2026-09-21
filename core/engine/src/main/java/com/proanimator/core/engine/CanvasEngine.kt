package com.proanimator.core.engine

import com.proanimator.domain.model.Layer
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.pow
import kotlin.math.sqrt

enum class ToolMode {
    DRAW,
    ERASE
}

/**
 * Core painting engine.
 * Manages layers, strokes, undo/redo, brush state and tools.
 */
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

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    private val _currentBrushId = MutableStateFlow("technical_pen")
    val currentBrushId: StateFlow<String> = _currentBrushId.asStateFlow()

    private val _currentColor = MutableStateFlow(0xFFFFFFFF)
    val currentColor: StateFlow<Long> = _currentColor.asStateFlow()

    private val _currentSize = MutableStateFlow(8f)
    val currentSize: StateFlow<Float> = _currentSize.asStateFlow()

    private val _toolMode = MutableStateFlow(ToolMode.DRAW)
    val toolMode: StateFlow<ToolMode> = _toolMode.asStateFlow()

    // Undo / Redo
    private val undoStack = mutableListOf<Map<String, List<Stroke>>>()
    private val redoStack = mutableListOf<Map<String, List<Stroke>>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun setToolMode(mode: ToolMode) {
        _toolMode.value = mode
    }

    fun setActiveLayer(layerId: String) {
        if (_layers.value.any { it.id == layerId }) {
            _activeLayerId.value = layerId
        }
    }

    fun setBrush(brushId: String) {
        _currentBrushId.value = brushId
        _toolMode.value = ToolMode.DRAW
    }

    fun setColor(color: Long) {
        _currentColor.value = color
        _toolMode.value = ToolMode.DRAW
    }

    fun setSize(size: Float) {
        _currentSize.value = size.coerceIn(1f, 200f)
    }

    fun startStroke(point: StrokePoint) {
        _currentStroke.value = listOf(point)
    }

    fun addPointToStroke(point: StrokePoint) {
        _currentStroke.update { current ->
            if (current.isEmpty()) listOf(point)
            else {
                // Simple distance-based filtering to reduce points
                val last = current.last()
                val dx = point.x - last.x
                val dy = point.y - last.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 1.5f) current + point else current
            }
        }
    }

    fun endStroke(): Stroke? {
        val rawPoints = _currentStroke.value
        if (rawPoints.size < 2) {
            _currentStroke.value = emptyList()
            return null
        }

        // Apply light smoothing
        val smoothed = smoothStroke(rawPoints)

        val isEraser = _toolMode.value == ToolMode.ERASE

        val stroke = Stroke(
            points = smoothed,
            brushId = if (isEraser) "eraser" else _currentBrushId.value,
            color = if (isEraser) 0x00000000 else _currentColor.value,
            size = _currentSize.value,
            opacity = if (isEraser) 1f else 1f
        )

        val layerId = _activeLayerId.value

        pushUndoState()

        if (isEraser) {
            // Simple eraser: remove strokes that intersect the eraser path (basic version)
            // For Phase 1 we just add an "eraser stroke" that will be rendered with clear blend later
            // For now we store it and handle visually in the UI
            _strokesByLayer.update { current ->
                val existing = current[layerId] ?: emptyList()
                current + (layerId to (existing + stroke))
            }
        } else {
            _strokesByLayer.update { current ->
                val existing = current[layerId] ?: emptyList()
                current + (layerId to (existing + stroke))
            }
        }

        _currentStroke.value = emptyList()
        redoStack.clear()
        updateUndoRedoState()

        return stroke
    }

    /** Very light moving-average smoothing */
    private fun smoothStroke(points: List<StrokePoint>): List<StrokePoint> {
        if (points.size < 3) return points

        val result = mutableListOf<StrokePoint>()
        result.add(points.first())

        for (i in 1 until points.lastIndex) {
            val prev = points[i - 1]
            val curr = points[i]
            val next = points[i + 1]

            val smoothedX = (prev.x + curr.x * 2 + next.x) / 4f
            val smoothedY = (prev.y + curr.y * 2 + next.y) / 4f
            val smoothedPressure = (prev.pressure + curr.pressure + next.pressure) / 3f

            result.add(
                StrokePoint(
                    x = smoothedX,
                    y = smoothedY,
                    pressure = smoothedPressure,
                    tiltX = curr.tiltX,
                    tiltY = curr.tiltY,
                    timestamp = curr.timestamp
                )
            )
        }

        result.add(points.last())
        return result
    }

    private fun pushUndoState() {
        undoStack.add(_strokesByLayer.value.toMap())
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(_strokesByLayer.value.toMap())
        val previous = undoStack.removeAt(undoStack.lastIndex)
        _strokesByLayer.value = previous
        updateUndoRedoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_strokesByLayer.value.toMap())
        val next = redoStack.removeAt(redoStack.lastIndex)
        _strokesByLayer.value = next
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun addLayer(name: String = "Layer ${_layers.value.size + 1}") {
        val newLayer = Layer(name = name)
        _layers.update { it + newLayer }
        _activeLayerId.value = newLayer.id
    }

    fun removeLayer(layerId: String) {
        if (_layers.value.size <= 1) return
        pushUndoState()
        _layers.update { it.filter { layer -> layer.id != layerId } }
        _strokesByLayer.update { it - layerId }
        if (_activeLayerId.value == layerId) {
            _activeLayerId.value = _layers.value.last().id
        }
        updateUndoRedoState()
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        _layers.update { list ->
            list.map {
                if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
            }
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        _layers.update { list ->
            list.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            }
        }
    }

    fun clearActiveLayer() {
        pushUndoState()
        val layerId = _activeLayerId.value
        _strokesByLayer.update { it + (layerId to emptyList()) }
        redoStack.clear()
        updateUndoRedoState()
    }
}
