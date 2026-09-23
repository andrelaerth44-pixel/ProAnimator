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

    private val _stabilization = MutableStateFlow(0.35f) // 0f = none, 1f = max
    val stabilization: StateFlow<Float> = _stabilization.asStateFlow()

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

    fun setStabilization(value: Float) {
        _stabilization.value = value.coerceIn(0f, 1f)
    }

    fun startStroke(point: StrokePoint) {
        _currentStroke.value = listOf(point)
    }

    fun addPointToStroke(point: StrokePoint) {
        _currentStroke.update { current ->
            if (current.isEmpty()) {
                listOf(point)
            } else {
                val last = current.last()
                val dx = point.x - last.x
                val dy = point.y - last.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 1.2f) current + point else current
            }
        }
    }

    fun endStroke(): Stroke? {
        val rawPoints = _currentStroke.value
        if (rawPoints.size < 2) {
            _currentStroke.value = emptyList()
            return null
        }

        val smoothed = smoothStroke(rawPoints, _stabilization.value)

        val isEraser = _toolMode.value == ToolMode.ERASE

        val stroke = Stroke(
            points = smoothed,
            brushId = if (isEraser) "eraser" else _currentBrushId.value,
            color = if (isEraser) 0x00000000 else _currentColor.value,
            size = _currentSize.value,
            opacity = 1f
        )

        val layerId = _activeLayerId.value

        pushUndoState()

        _strokesByLayer.update { current ->
            val existing = current[layerId] ?: emptyList()
            current + (layerId to (existing + stroke))
        }

        _currentStroke.value = emptyList()
        redoStack.clear()
        updateUndoRedoState()

        return stroke
    }

    private fun smoothStroke(points: List<StrokePoint>, amount: Float): List<StrokePoint> {
        if (points.size < 3 || amount <= 0f) return points

        val result = mutableListOf<StrokePoint>()
        result.add(points.first())

        val window = (2 + (amount * 4)).toInt().coerceIn(2, 6)

        for (i in 1 until points.lastIndex) {
            var sumX = 0f
            var sumY = 0f
            var sumP = 0f
            var count = 0

            for (j in (i - window)..(i + window)) {
                if (j in points.indices) {
                    sumX += points[j].x
                    sumY += points[j].y
                    sumP += points[j].pressure
                    count++
                }
            }

            result.add(
                StrokePoint(
                    x = sumX / count,
                    y = sumY / count,
                    pressure = sumP / count,
                    tiltX = points[i].tiltX,
                    tiltY = points[i].tiltY,
                    timestamp = points[i].timestamp
                )
            )
        }

        result.add(points.last())
        return result
    }

    private fun pushUndoState() {
        undoStack.add(_strokesByLayer.value.toMap())
        if (undoStack.size > 50) undoStack.removeAt(0)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(_strokesByLayer.value.toMap())
        _strokesByLayer.value = undoStack.removeAt(undoStack.lastIndex)
        updateUndoRedoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_strokesByLayer.value.toMap())
        _strokesByLayer.value = redoStack.removeAt(redoStack.lastIndex)
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
        _layers.update { it.filter { it.id != layerId } }
        _strokesByLayer.update { it - layerId }
        if (_activeLayerId.value == layerId) {
            _activeLayerId.value = _layers.value.last().id
        }
        updateUndoRedoState()
    }

    fun toggleLayerVisibility(layerId: String) {
        _layers.update { list ->
            list.map { if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it }
        }
    }

    fun clearActiveLayer() {
        pushUndoState()
        val layerId = _activeLayerId.value
        _strokesByLayer.update { it + (layerId to emptyList()) }
        redoStack.clear()
        updateUndoRedoState()
    }

    // === Serialization helpers for Save/Load ===

    fun getSerializableState(): EngineState {
        return EngineState(
            layers = _layers.value,
            activeLayerId = _activeLayerId.value,
            strokesByLayer = _strokesByLayer.value
        )
    }

    fun loadState(state: EngineState) {
        pushUndoState()
        _layers.value = state.layers
        _activeLayerId.value = state.activeLayerId
        _strokesByLayer.value = state.strokesByLayer
        redoStack.clear()
        updateUndoRedoState()
    }

    data class EngineState(
        val layers: List<Layer>,
        val activeLayerId: String,
        val strokesByLayer: Map<String, List<Stroke>>
    )
}
