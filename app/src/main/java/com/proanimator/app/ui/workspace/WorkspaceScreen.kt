package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.brushes.DefaultBrushes
import com.proanimator.core.engine.CanvasEngine
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint

@Composable
fun WorkspaceScreen() {
    val engine = remember { CanvasEngine(width = 1920, height = 1080) }

    val layers by engine.layers.collectAsState()
    val activeLayerId by engine.activeLayerId.collectAsState()
    val strokesByLayer by engine.strokesByLayer.collectAsState()
    val currentStroke by engine.currentStroke.collectAsState()
    val currentBrushId by engine.currentBrushId.collectAsState()
    val currentSize by engine.currentSize.collectAsState()
    val currentColor by engine.currentColor.collectAsState()
    val canUndo by engine.canUndo.collectAsState()
    val canRedo by engine.canRedo.collectAsState()

    val colors = listOf(
        0xFFFFFFFF, // White
        0xFF000000, // Black
        0xFFFF5252, // Red
        0xFFFF9800, // Orange
        0xFFFFEB3B, // Yellow
        0xFF4CAF50, // Green
        0xFF2196F3, // Blue
        0xFF9C27B0, // Purple
        0xFFE91E63  // Pink
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // === TOP BAR ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ProAnimator",
                color = Color(0xFFBB86FC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Undo
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (canUndo) Color(0xFF333333) else Color(0xFF222222))
                        .clickable(enabled = canUndo) { engine.undo() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Undo", color = if (canUndo) Color.White else Color.Gray, fontSize = 13.sp)
                }

                // Redo
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (canRedo) Color(0xFF333333) else Color(0xFF222222))
                        .clickable(enabled = canRedo) { engine.redo() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Redo", color = if (canRedo) Color.White else Color.Gray, fontSize = 13.sp)
                }
            }
        }

        // === BRUSH + COLOR + SIZE BAR ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(vertical = 8.dp)
        ) {
            // Brushes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DefaultBrushes.all.forEach { brush ->
                    val isSelected = currentBrushId == brush.id

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF7C4DFF) else Color(0xFF333333))
                            .clickable {
                                engine.setBrush(brush.id)
                                engine.setSize(brush.defaultSize)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = brush.name, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Colors + Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color swatches
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { colorLong ->
                        val isSelected = currentColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { engine.setColor(colorLong) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Size slider
                Text("Size", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = currentSize,
                    onValueChange = { engine.setSize(it) },
                    valueRange = 1f..80f,
                    modifier = Modifier.width(140.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFBB86FC),
                        activeTrackColor = Color(0xFF7C4DFF)
                    )
                )
                Text(
                    text = "${currentSize.toInt()}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.width(28.dp)
                )
            }
        }

        // === MAIN AREA ===
        Row(modifier = Modifier.weight(1f)) {

            // Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF2C2C2C))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    engine.startStroke(
                                        StrokePoint(x = offset.x, y = offset.y, pressure = 1f)
                                    )
                                },
                                onDrag = { change, _ ->
                                    engine.addPointToStroke(
                                        StrokePoint(
                                            x = change.position.x,
                                            y = change.position.y,
                                            pressure = 1f // real pressure later
                                        )
                                    )
                                },
                                onDragEnd = { engine.endStroke() }
                            )
                        }
                ) {
                    // Draw committed strokes
                    layers.forEach { layer ->
                        if (!layer.isVisible) return@forEach
                        val strokes = strokesByLayer[layer.id] ?: emptyList()
                        strokes.forEach { stroke ->
                            drawStroke(stroke, layer.opacity)
                        }
                    }

                    // Current stroke
                    if (currentStroke.size > 1) {
                        val path = Path()
                        path.moveTo(currentStroke[0].x, currentStroke[0].y)
                        for (i in 1 until currentStroke.size) {
                            path.lineTo(currentStroke[i].x, currentStroke[i].y)
                        }
                        drawPath(
                            path = path,
                            color = Color(currentColor),
                            style = Stroke(
                                width = currentSize,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (strokesByLayer.values.all { it.isEmpty() } && currentStroke.isEmpty()) {
                    Text(
                        text = "Draw with finger or stylus",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // === LAYER PANEL ===
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1A1A))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Layers",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                layers.asReversed().forEach { layer ->
                    val isActive = layer.id == activeLayerId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) Color(0xFF3A2A5A) else Color.Transparent)
                            .clickable { engine.setActiveLayer(layer.id) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (layer.isVisible) Color(0xFF03DAC6) else Color.Gray)
                                .clickable { engine.toggleLayerVisibility(layer.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = layer.name,
                            color = if (isActive) Color.White else Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF333333))
                        .clickable { engine.addLayer() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Add Layer", color = Color.White, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF442222))
                        .clickable { engine.clearActiveLayer() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Clear Layer", color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }
        }

        // === BOTTOM ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color(0xFF141414)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Timeline · Phase 2",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: Stroke,
    layerOpacity: Float
) {
    if (stroke.points.size < 2) return

    val path = Path()
    path.moveTo(stroke.points[0].x, stroke.points[0].y)
    for (i in 1 until stroke.points.size) {
        path.lineTo(stroke.points[i].x, stroke.points[i].y)
    }

    val color = Color(stroke.color).copy(alpha = layerOpacity * stroke.opacity)

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.size,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
