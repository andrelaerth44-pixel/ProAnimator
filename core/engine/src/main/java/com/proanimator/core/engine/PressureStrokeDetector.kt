package com.proanimator.core.engine

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import com.proanimator.domain.model.StrokePoint

/**
 * Stroke with pressure + palm rejection (Compose-safe, no MotionEvent internals).
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
        val canvas0 = toCanvas(down.position)
        onStart(StrokePoint(canvas0.x, canvas0.y, pressureOf(down)))

        var cancelled = false
        while (!cancelled) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val changes = event.changes

            // Pinch / multi-touch → cancel stroke
            if (changes.count { it.pressed } > 1) {
                cancelled = true
                onCancel()
                break
            }

            var anyPressed = false
            changes.forEach { change ->
                if (change.pressed) {
                    anyPressed = true
                    if (isStylus && change.type == PointerType.Touch) {
                        change.consume()
                        return@forEach
                    }
                    val c = toCanvas(change.position)
                    onMove(StrokePoint(c.x, c.y, pressureOf(change)))
                    change.consume()
                } else if (change.changedToUp()) {
                    change.consume()
                }
            }

            if (!anyPressed) break
        }

        if (!cancelled) onEnd()
    }
}

private fun pressureOf(change: PointerInputChange): Float {
    val raw = change.pressure
    if (raw <= 0.01f && change.type != PointerType.Stylus) return 1f
    return if (raw > 1f) (raw / 1.5f).coerceIn(0.05f, 1f)
    else raw.coerceIn(0.05f, 1f)
}
