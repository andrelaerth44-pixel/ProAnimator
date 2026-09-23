package com.proanimator.core.engine

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import com.proanimator.domain.model.StrokePoint

/**
 * Stroke with pressure + palm rejection (no internal MotionEvent API).
 */
suspend fun PointerInputScope.detectPressureStroke(
    onStart: (StrokePoint) -> Unit,
    onMove: (StrokePoint) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit = onEnd,
    toCanvas: (Offset) -> Offset
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()

        val isStylus = down.type == PointerType.Stylus

        val pressure0 = normalizePressure(down)
        val canvas0 = toCanvas(down.position)
        onStart(StrokePoint(canvas0.x, canvas0.y, pressure0))

        var cancelled = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)

            // Multi-touch: abort (pinch owns the gesture)
            if (event.changes.count { it.pressed } > 1) {
                cancelled = true
                onCancel()
                return@awaitEachGesture
            }

            event.changes.forEach { change ->
                if (!change.pressed) return@forEach
                if (isStylus && change.type == PointerType.Touch) {
                    change.consume()
                    return@forEach
                }
                val p = normalizePressure(change)
                val c = toCanvas(change.position)
                onMove(StrokePoint(c.x, c.y, p))
                change.consume()
            }
        } while (event.changes.any { it.pressed } && !cancelled)

        if (!cancelled) onEnd()
    }
}

private fun normalizePressure(change: PointerInputChange): Float {
    val raw = change.pressure
    if (raw <= 0.01f && change.type != PointerType.Stylus) return 1f
    return if (raw > 1f) (raw / 1.5f).coerceIn(0.05f, 1f)
    else raw.coerceIn(0.05f, 1f)
}
