package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.timeline.EasingType
import com.proanimator.core.timeline.Keyframe
import com.proanimator.core.timeline.KeyframeInterpolator
import kotlin.math.pow

/**
 * Visual cubic-Bezier easing editor (CSS-style).
 * Control points P1 (x1,y1) and P2 (x2,y2) in unit square.
 * Inspired by cubic-bezier.com + Compose CubicBezierEasing research.
 */
@Composable
fun BezierEditor(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    onChange: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var p1 by remember(x1, y1) { mutableStateOf(Offset(x1, y1)) }
    var p2 by remember(x2, y2) { mutableStateOf(Offset(x2, y2)) }
    var dragging by remember { mutableStateOf(0) } // 0=none, 1=p1, 2=p2

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        Text("Bezier Easing", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A))
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val s1 = Offset(p1.x * w, (1f - p1.y) * h)
                                val s2 = Offset(p2.x * w, (1f - p2.y) * h)
                                val d1 = (offset - s1).getDistance()
                                val d2 = (offset - s2).getDistance()
                                dragging = when {
                                    d1 < 28f && d1 <= d2 -> 1
                                    d2 < 28f -> 2
                                    else -> 0
                                }
                            },
                            onDragEnd = { dragging = 0 },
                            onDragCancel = { dragging = 0 },
                            onDrag = { change, _ ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val nx = (change.position.x / w).coerceIn(0f, 1f)
                                val ny = (1f - change.position.y / h).coerceIn(-0.5f, 1.5f)
                                when (dragging) {
                                    1 -> {
                                        p1 = Offset(nx, ny)
                                        onChange(p1.x, p1.y, p2.x, p2.y)
                                    }
                                    2 -> {
                                        p2 = Offset(nx, ny)
                                        onChange(p1.x, p1.y, p2.x, p2.y)
                                    }
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Grid
                for (i in 0..4) {
                    val gx = w * i / 4f
                    val gy = h * i / 4f
                    drawLine(Color(0xFF3A3A3A), Offset(gx, 0f), Offset(gx, h), 1f)
                    drawLine(Color(0xFF3A3A3A), Offset(0f, gy), Offset(w, gy), 1f)
                }

                val s0 = Offset(0f, h)
                val s1 = Offset(p1.x * w, (1f - p1.y) * h)
                val s2 = Offset(p2.x * w, (1f - p2.y) * h)
                val s3 = Offset(w, 0f)

                // Handles
                drawLine(
                    Color(0xFF666666), s0, s1, 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
                drawLine(
                    Color(0xFF666666), s3, s2, 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )

                // Curve
                val curve = Path().apply {
                    moveTo(s0.x, s0.y)
                    cubicTo(s1.x, s1.y, s2.x, s2.y, s3.x, s3.y)
                }
                drawPath(
                    curve,
                    Color(0xFFBB86FC),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // Endpoints
                drawCircle(Color(0xFF03DAC6), 5f, s0)
                drawCircle(Color(0xFF03DAC6), 5f, s3)

                // Control points
                drawCircle(Color(0xFFFF5252), 8f, s1)
                drawCircle(Color(0xFF69F0AE), 8f, s2)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "P1(%.2f, %.2f)  P2(%.2f, %.2f)".format(p1.x, p1.y, p2.x, p2.y),
            color = Color.Gray,
            fontSize = 9.sp
        )

        // Presets
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BezierPresetChip("Ease") {
                p1 = Offset(0.25f, 0.1f); p2 = Offset(0.25f, 1f)
                onChange(p1.x, p1.y, p2.x, p2.y)
            }
            BezierPresetChip("In") {
                p1 = Offset(0.55f, 0.06f); p2 = Offset(0.68f, 0.19f)
                onChange(p1.x, p1.y, p2.x, p2.y)
            }
            BezierPresetChip("Out") {
                p1 = Offset(0.22f, 0.61f); p2 = Offset(0.36f, 1f)
                onChange(p1.x, p1.y, p2.x, p2.y)
            }
            BezierPresetChip("InOut") {
                p1 = Offset(0.65f, 0.05f); p2 = Offset(0.36f, 1f)
                onChange(p1.x, p1.y, p2.x, p2.y)
            }
            BezierPresetChip("Linear") {
                p1 = Offset(0f, 0f); p2 = Offset(1f, 1f)
                onChange(p1.x, p1.y, p2.x, p2.y)
            }
        }
    }
}

@Composable
private fun BezierPresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, color = Color.White, fontSize = 9.sp)
    }
}
