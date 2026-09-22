package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                                val nx = (offset.x / w).coerceIn(0f, 1f)
                                val ny = 1f - (offset.y / h).coerceIn(0f, 1f)
                                val d1 = (nx - p1.x).pow(2) + (ny - p1.y).pow(2)
                                val d2 = (nx - p2.x).pow(2) + (ny - p2.y).pow(2)
                                dragging = if (d1 <= d2) 1 else 2
                            },
                            onDrag = { change, _ ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val nx = (change.position.x / w).coerceIn(0f, 1f)
                                val ny = (1f - change.position.y / h).coerceIn(-0.5f, 1.5f)
                                if (dragging == 1) {
                                    p1 = Offset(nx, ny)
                                } else if (dragging == 2) {
                                    p2 = Offset(nx, ny)
                                }
                                onChange(p1.x, p1.y, p2.x, p2.y)
                            },
                            onDragEnd = { dragging = 0 }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Grid
                for (i in 1..3) {
                    val f = i / 4f
                    drawLine(Color(0xFF3A3A3A), Offset(f * w, 0f), Offset(f * w, h), 1f)
                    drawLine(Color(0xFF3A3A3A), Offset(0f, f * h), Offset(w, f * h), 1f)
                }

                fun toScreen(u: Float, v: Float) = Offset(u * w, (1f - v) * h)

                val s0 = toScreen(0f, 0f)
                val s1 = toScreen(p1.x, p1.y)
                val s2 = toScreen(p2.x, p2.y)
                val s3 = toScreen(1f, 1f)

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

// Need clickable import
private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(onClick = onClick)
    )
