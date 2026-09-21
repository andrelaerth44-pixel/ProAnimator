package com.proanimator.app.ui.workspace

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.brushes.DefaultBrushes
import com.proanimator.core.engine.CanvasEngine
import com.proanimator.core.engine.ToolMode
import com.proanimator.core.fileformat.ProjectSerializer
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import java.io.File

@Composable
fun WorkspaceScreen() {
    val context = LocalContext.current
    val engine = remember { CanvasEngine(width = 1920, height = 1080) }

    val layers by engine.layers.collectAsState()
    val activeLayerId by engine.activeLayerId.collectAsState()
    val strokesByLayer by engine.strokesByLayer.collectAsState()
    val currentStroke by engine.currentStroke.collectAsState()
    val currentBrushId by engine.currentBrushId.collectAsState()
    val currentSize by engine.currentSize.collectAsState()
    val currentColor by engine.currentColor.collectAsState()
    val toolMode by engine.toolMode.collectAsState()
    val canUndo by engine.canUndo.collectAsState()
    val canRedo by engine.canRedo.collectAsState()
    val stabilization by engine.stabilization.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    val colors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFF9800,
        0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ProAnimator", color = Color(0xFFBB86FC), fontSize = 17.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolButton("Draw", selected = toolMode == ToolMode.DRAW) { engine.setToolMode(ToolMode.DRAW) }
                ToolButton("Eraser", selected = toolMode == ToolMode.ERASE) { engine.setToolMode(ToolMode.ERASE) }
                ToolButton("Undo", enabled = canUndo) { engine.undo() }
                ToolButton("Redo", enabled = canRedo) { engine.redo() }
                ToolButton("Save") {
                    try {
                        val json = ProjectSerializer.toJson(engine, "MyProject")
                        val file = File(context.filesDir, "project.json")
                        file.writeText(json)
                        statusMessage = "Saved!"
                    } catch (e: Exception) {
                        statusMessage = "Save error"
                    }
                }
                ToolButton("Load") {
                    try {
                        val file = File(context.filesDir, "project.json")
                        if (file.exists()) {
                            ProjectSerializer.fromJson(file.readText(), engine)
                            statusMessage = "Loaded!"
                        } else {
                            statusMessage = "No save found"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Load error"
                    }
                }
            }
        }

        // Status
        statusMessage?.let {
            Text(
                text = it,
                color = Color(0xFF03DAC6),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(2000)
                statusMessage = null
            }
        }

        // TOOLS BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(vertical = 8.dp)
        ) {
            if (toolMode == ToolMode.DRAW) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultBrushes.all.forEach { brush ->
                        val selected = currentBrushId == brush.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color(0xFF7C4DFF) else Color(0xFF333333))
                                .clickable {
                                    engine.setBrush(brush.id)
                                    engine.setSize(brush.defaultSize)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(brush.name, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        colors.forEach { c ->
                            val selected = currentColor == c
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                                    .clickable { engine.setColor(c) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(if (toolMode == ToolMode.ERASE) "Eraser" else "Size", color = Color.LightGray, fontSize = 11.sp)
                Slider(
                    value = currentSize,
                    onValueChange = { engine.setSize(it) },
                    valueRange = 1f..100f,
                    modifier = Modifier.width(110.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFBB86FC), activeTrackColor = Color(0xFF7C4DFF))
                )
                Text("${currentSize.toInt()}", color = Color.White, fontSize = 11.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Text("Smooth", color = Color.LightGray, fontSize = 11.sp)
                Slider(
                    value = stabilization,
                    onValueChange = { engine.setStabilization(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(90.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF03DAC6), activeTrackColor = Color(0xFF018786))
                )
            }
        }

        // MAIN
        Row(modifier = Modifier.weight(1f)) {
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
                                    engine.startStroke(StrokePoint(offset.x, offset.y, pressure = 1f))
                                },
                                onDrag = { change, _ ->
                                    engine.addPointToStroke(
                                        StrokePoint(change.position.x, change.position.y, pressure = 1f)
                                    )
                                },
                                onDragEnd = { engine.endStroke() }
                            )
                        }
                ) {
                    layers.forEach { layer ->
                        if (!layer.isVisible) return@forEach
                        (strokesByLayer[layer.id] ?: emptyList()).forEach { stroke ->
                            drawStroke(stroke, layer.opacity)
                        }
                    }

                    if (currentStroke.size > 1) {
                        val path = Path().apply {
                            moveTo(currentStroke[0].x, currentStroke[0].y)
                            for (i in 1 until currentStroke.size) lineTo(currentStroke[i].x, currentStroke[i].y)
                        }
                        val color = if (toolMode == ToolMode.ERASE) Color.Gray.copy(alpha = 0.45f) else Color(currentColor)
                        drawPath(path, color, style = Stroke(currentSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                if (strokesByLayer.values.all { it.isEmpty() } && currentStroke.isEmpty()) {
                    Text("Draw · Erase · Save / Load", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                }
            }

            // Layers
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1A1A))
                    .padding(8.dp)
            ) {
                Text("Layers", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                layers.asReversed().forEach { layer ->
                    val active = layer.id == activeLayerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) Color(0xFF3A2A5A) else Color.Transparent)
                            .clickable { engine.setActiveLayer(layer.id) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(if (layer.isVisible) Color(0xFF03DAC6) else Color.Gray)
                                .clickable { engine.toggleLayerVisibility(layer.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(layer.name, color = if (active) Color.White else Color.LightGray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF333333))
                        .clickable { engine.addLayer() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) { Text("+ Layer", color = Color.White, fontSize = 12.sp) }

                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF442222))
                        .clickable { engine.clearActiveLayer() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Clear", color = Color(0xFFFF8A80), fontSize = 12.sp) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color(0xFF141414)),
            contentAlignment = Alignment.Center
        ) {
            Text("Timeline · Phase 2 coming next", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ToolButton(
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    selected -> Color(0xFF7C4DFF)
                    enabled -> Color(0xFF333333)
                    else -> Color(0xFF222222)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 12.sp)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: Stroke, layerOpacity: Float) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) lineTo(stroke.points[i].x, stroke.points[i].y)
    }

    if (stroke.brushId == "eraser") {
        drawPath(path, Color.DarkGray.copy(alpha = 0.3f * layerOpacity), style = Stroke(stroke.size, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
        drawPath(path, Color(stroke.color).copy(alpha = layerOpacity * stroke.opacity), style = Stroke(stroke.size, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
