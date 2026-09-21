package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.engine.CanvasEngine
import com.proanimator.domain.model.StrokePoint

@Composable
fun WorkspaceScreen() {
    val engine = remember { CanvasEngine(width = 1920, height = 1080) }
    val currentStroke by engine.currentStroke.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ProAnimator  ·  Phase 1",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        // Canvas area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                engine.startStroke(
                                    StrokePoint(
                                        x = offset.x,
                                        y = offset.y,
                                        pressure = 1f
                                    )
                                )
                            },
                            onDrag = { change, _ ->
                                engine.addPointToStroke(
                                    StrokePoint(
                                        x = change.position.x,
                                        y = change.position.y,
                                        pressure = 1f // TODO: real pressure from MotionEvent
                                    )
                                )
                            },
                            onDragEnd = {
                                engine.endStroke(
                                    brushId = "default",
                                    color = 0xFFFFFFFF,
                                    size = 8f
                                )
                            }
                        )
                    }
            ) {
                // Draw current stroke
                if (currentStroke.size > 1) {
                    val path = Path()
                    path.moveTo(currentStroke[0].x, currentStroke[0].y)
                    for (i in 1 until currentStroke.size) {
                        path.lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Hint
            Text(
                text = "Draw here · Stylus / Finger",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Bottom timeline placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Timeline (Phase 2)",
                color = Color.Gray
            )
        }
    }
}
