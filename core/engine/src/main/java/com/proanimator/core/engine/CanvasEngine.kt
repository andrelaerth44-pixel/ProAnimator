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
 * Manages layers, strokes persistence and history.
 * Designed for real-time performance.
 */
class CanvasEngine(
    val width: Int,
    val height: Int
) {
    private val _layers = MutableStateFlow<List<Layer>>(listOf(Layer(name = "Layer 1")))
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()

    private val _activeLayerId = MutableStateFlow(_layers.value.first().id)
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    // Strokes stored per layer
    private val _strokesByLayer = MutableStateFlow<Map<String, List<Stroke>>>(emptyMap())
    val strokesByLayer: StateFlow<Map<String, List<Stroke>>> = _strokesByLayer.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    // Current brush settings
    private val _currentBrushId = MutableStateFlow("technical_pen")
    val currentBrushId: StateFlow<String> = _currentBrushId.asStateFlow()

    private val _currentColor = MutableStateFlow(0xFFFFFFFF)
    val currentColor: StateFlow<Long> = _currentColor.asStateFlow()

    private val _currentSize = MutableStateFlow(8f)
    val currentSize: StateFlow<Float> = _currentSize.asStateFlow()

    private val history = mutableListOf<EngineCommand>()
    private var historyIndex = -1

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

        // Commit stroke to the active layer
        _strokesByLayer.update { current ->
            val existing = current[layerId] ?: emptyList()
            current + (layerId to (existing + stroke))
        }

        // Clear temporary stroke
        _currentStroke.value = emptyList()

        // TODO: Push to history for undo

        return stroke
    }

    fun addLayer(name: String = "Layer ${_layers.value.size + 1}") {
        val newLayer = Layer(name = name)
        _layers.update { it + newLayer }
        _activeLayerId.value = newLayer.id
    }

    fun removeLayer(layerId: String) {
        if (_layers.value.size <= 1) return

        _layers.update { it.filter { layer -> layer.id != layerId } }
        _strokesByLayer.update { it - layerId }

        if (_activeLayerId.value == layerId) {
            _activeLayerId.value = _layers.value.last().id
        }
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
        val layerId = _activeLayerId.value
        _strokesByLayer.update { it + (layerId to emptyList()) }
    }

    fun undo() {
        if (historyIndex >= 0) {
            history[historyIndex].undo()
            historyIndex--
        }
    }

    fun redo() {
        if (historyIndex < history.lastIndex) {
            historyIndex++
            history[historyIndex].execute()
        }
    }

    interface EngineCommand {
        fun execute()
        fun undo()
    }
}
