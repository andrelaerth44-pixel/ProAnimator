package com.proanimator.core.engine

import com.proanimator.domain.model.Layer
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Core painting engine.
 * Manages layers, strokes persistence, undo/redo and brush state.
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

    // Simple undo stack (stores previous strokes map)
    private val undoStack = mutableListOf<Map<String, List<Stroke>>>()
    private val redoStack = mutableListOf<Map<String, List<Stroke>>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun setActiveLayer(layerId: String) {
        if (_layers.value.any { it.id == layerId }) {
            _activeLayerId.value = layerId
        }
    }

    fun setBrush(brushId: String) {
        _currentBrushId.value = brushId
    }

    fun setColor(color: Long) {
        _currentColor.value = color
    }

    fun setSize(size: Float) {
        _currentSize.value = size.coerceIn(1f, 200f)
    }

    fun startStroke(point: StrokePoint) {
        _currentStroke.value = listOf(point)
    }

    fun addPointToStroke(point: StrokePoint) {
        _currentStroke.update { it + point }
    }

    fun endStroke(): Stroke? {
        val points = _currentStroke.value
        if (points.size < 2) {
            _currentStroke.value = emptyList()
            return null
        }

        val stroke = Stroke(
            points = points,
            brushId = _currentBrushId.value,
            color = _currentColor.value,
            size = _currentSize.value,
            opacity = 1f
        )

        val layerId = _activeLayerId.value

        // Save state for undo
        pushUndoState()

        // Commit stroke
        _strokesByLayer.update { current ->
            val existing = current[layerId] ?: emptyList()
            current + (layerId to (existing + stroke))
        }

        _currentStroke.value = emptyList()
        redoStack.clear()
        updateUndoRedoState()

        return stroke
    }

    private fun pushUndoState() {
        undoStack.add(_strokesByLayer.value.toMap())
        if (undoStack.size > 50) { // limit history
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
