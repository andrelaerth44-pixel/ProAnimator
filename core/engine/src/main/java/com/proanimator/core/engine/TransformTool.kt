package com.proanimator.core.engine

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Transform tool — move / scale / rotate the active layer content in-place.
 * Applies affine update to layer bitmap origin tracking (logical transform).
 */
class TransformTool {

    data class State(
        val enabled: Boolean = false,
        val tx: Float = 0f,
        val ty: Float = 0f,
        val scale: Float = 1f,
        val rotation: Float = 0f
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun enable() { _state.value = _state.value.copy(enabled = true) }
    fun disable() { _state.value = State() }
    fun toggle() {
        _state.value = if (_state.value.enabled) State() else State(enabled = true)
    }

    fun drag(dx: Float, dy: Float) {
        if (!_state.value.enabled) return
        _state.value = _state.value.copy(
            tx = _state.value.tx + dx,
            ty = _state.value.ty + dy
        )
    }

    fun pinch(zoom: Float) {
        if (!_state.value.enabled) return
        _state.value = _state.value.copy(
            scale = (_state.value.scale * zoom).coerceIn(0.1f, 8f)
        )
    }

    fun rotate(deltaDeg: Float) {
        if (!_state.value.enabled) return
        _state.value = _state.value.copy(rotation = _state.value.rotation + deltaDeg)
    }

    fun reset() {
        _state.value = _state.value.copy(tx = 0f, ty = 0f, scale = 1f, rotation = 0f)
    }
}
