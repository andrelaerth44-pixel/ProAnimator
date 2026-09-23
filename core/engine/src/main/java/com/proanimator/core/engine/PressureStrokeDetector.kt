package com.proanimator.core.engine

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import com.proanimator.domain.model.StrokePoint

/**
 * Single-finger / stylus stroke with real pressure.
 *
 * Research:
 * - PointerInputChange.pressure (Compose 1.3+)
 * - PointerType.Stylus vs Touch
 * - Finger often reports pressure=1.0; stylus 0..1+
 */
suspend fun PointerInputScope.detectPressureStroke(
    onStart: (StrokePoint) -> Unit,
    onMove: (StrokePoint) -> Unit,
    onEnd: () -> Unit,
    toCanvas: (Offset) -> Offset
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // Multi-touch: let zoom/pan handle (don't start stroke if 2+ pointers soon)
        down.consume()

        val pressure0 = normalizePressure(down)
        val canvas0 = toCanvas(down.position)
        onStart(StrokePoint(canvas0.x, canvas0.y, pressure0))

        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            // If second finger down, abort stroke (pinch)
            if (event.changes.count { it.pressed } > 1) {
                onEnd()
                return@awaitEachGesture
            }
            event.changes.forEach { change ->
                if (change.pressed) {
                    val p = normalizePressure(change)
                    val c = toCanvas(change.position)
                    onMove(StrokePoint(c.x, c.y, p))
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        onEnd()
    }
}

private fun normalizePressure(change: PointerInputChange): Float {
    val raw = change.pressure
    // Some devices report 0 for finger; treat as 1
    if (raw <= 0.01f && change.type != PointerType.Stylus) return 1f
    // Stylus can exceed 1.0 on some panels
    return raw.coerceIn(0.05f, 1.5f).coerceAtMost(1f).let {
        // Keep some headroom if device gives >1
        if (raw > 1f) (raw / 1.5f).coerceIn(0.05f, 1f) else it
    }
}
