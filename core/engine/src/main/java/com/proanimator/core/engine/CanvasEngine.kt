package com.proanimator.core.engine

import com.proanimator.domain.model.Layer
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core painting engine.
 * Responsible for managing layers, strokes and history.
 * Designed for real-time performance.
 */
class CanvasEngine(
    val width: Int,
    val height: Int
) {
    private val _layers = MutableStateFlow<List<Layer>>(listOf(Layer(name = "Background")))
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    private val history = mutableListOf<EngineCommand>()
    private var historyIndex = -1

    fun startStroke(point: StrokePoint) {
        _currentStroke.value = listOf(point)
    }

    fun addPointToStroke(point: StrokePoint) {
        _currentStroke.value = _currentStroke.value + point
    }

    fun endStroke(brushId: String, color: Long, size: Float, opacity: Float = 1f): Stroke? {
        val points = _currentStroke.value
        if (points.isEmpty()) return null

        val stroke = Stroke(
            points = points,
            brushId = brushId,
            color = color,
            size = size,
            opacity = opacity
        )

        // TODO: Commit stroke to active layer bitmap
        // For now we just clear the temporary stroke
        _currentStroke.value = emptyList()

        return stroke
    }

    fun addLayer(name: String = "Layer ${_layers.value.size + 1}") {
        val newLayer = Layer(name = name)
        _layers.value = _layers.value + newLayer
    }

    fun removeLayer(layerId: String) {
        if (_layers.value.size <= 1) return
        _layers.value = _layers.value.filter { it.id != layerId }
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
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

    // Command pattern for undo/redo
    interface EngineCommand {
        fun execute()
        fun undo()
    }
}
