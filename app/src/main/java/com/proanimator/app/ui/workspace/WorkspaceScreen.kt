package com.proanimator.app.ui.workspace

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
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
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun WorkspaceScreen() {
    val context = LocalContext.current
    val canvasEngine = remember { CanvasEngine(width = 1920, height = 1080) }
    val timeline = remember { TimelineEngine() }

    val layers by canvasEngine.layers.collectAsState()
    val activeLayerId by canvasEngine.activeLayerId.collectAsState()
    val strokesByLayer by canvasEngine.strokesByLayer.collectAsState()
    val currentStroke by canvasEngine.currentStroke.collectAsState()
    val currentBrushId by canvasEngine.currentBrushId.collectAsState()
    val currentSize by canvasEngine.currentSize.collectAsState()
    val currentColor by canvasEngine.currentColor.collectAsState()
    val toolMode by canvasEngine.toolMode.collectAsState()
    val canUndo by canvasEngine.canUndo.collectAsState()
    val canRedo by canvasEngine.canRedo.collectAsState()
    val stabilization by canvasEngine.stabilization.collectAsState()

    val currentFrame by timeline.currentFrame.collectAsState()
    val isPlaying by timeline.isPlaying.collectAsState()
    val durationFrames by timeline.durationFrames.collectAsState()
    val fps by timeline.fps.collectAsState()
    val timelineMode by timeline.mode.collectAsState()
    val tracks by timeline.tracks.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Simple playback ticker
    LaunchedEffect(isPlaying, fps) {
        while (isPlaying) {
            delay((1000f / fps).toLong().coerceAtLeast(16))
            timeline.tick()
        }
    }

    val colors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFF9800,
        0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // === TOP BAR ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ProAnimator", color = Color(0xFFBB86FC), fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ToolButton("Draw", selected = toolMode == ToolMode.DRAW) { canvasEngine.setToolMode(ToolMode.DRAW) }
                ToolButton("Eraser", selected = toolMode == ToolMode.ERASE) { canvasEngine.setToolMode(ToolMode.ERASE) }
                ToolButton("Undo", enabled = canUndo) { canvasEngine.undo() }
                ToolButton("Redo", enabled = canRedo) { canvasEngine.redo() }
                ToolButton("Save") {
                    try {
                        val json = ProjectSerializer.toJson(canvasEngine)
                        File(context.filesDir, "project.json").writeText(json)
                        statusMessage = "Saved"
                    } catch (e: Exception) { statusMessage = "Save error" }
                }
                ToolButton("Load") {
                    try {
                        val file = File(context.filesDir, "project.json")
                        if (file.exists()) {
                            ProjectSerializer.fromJson(file.readText(), canvasEngine)
                            statusMessage = "Loaded"
                        } else statusMessage = "No save"
                    } catch (e: Exception) { statusMessage = "Load error" }
                }
            }
        }

        statusMessage?.let {
            Text(it, color = Color(0xFF03DAC6), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 12.dp, vertical = 3.dp))
            LaunchedEffect(it) { delay(1800); statusMessage = null }
        }

        // === TOOLS BAR ===
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(vertical = 6.dp)) {
            if (toolMode == ToolMode.DRAW) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DefaultBrushes.all.forEach { brush ->
                        val selected = currentBrushId == brush.id
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Color(0xFF7C4DFF) else Color(0xFF333333))
                            .clickable { canvasEngine.setBrush(brush.id); canvasEngine.setSize(brush.defaultSize) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(brush.name, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { c ->
                            val selected = currentColor == c
                            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(c))
                                .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                                .clickable { canvasEngine.setColor(c) })
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(if (toolMode == ToolMode.ERASE) "Eraser" else "Size", color = Color.LightGray, fontSize = 10.sp)
                Slider(value = currentSize, onValueChange = { canvasEngine.setSize(it) }, valueRange = 1f..80f,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFBB86FC), activeTrackColor = Color(0xFF7C4DFF)))
                Text("${currentSize.toInt()}", color = Color.White, fontSize = 10.sp)

                Spacer(modifier = Modifier.width(8.dp))
                Text("Smooth", color = Color.LightGray, fontSize = 10.sp)
                Slider(value = stabilization, onValueChange = { canvasEngine.setStabilization(it) }, valueRange = 0f..1f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF03DAC6), activeTrackColor = Color(0xFF018786)))
            }
        }

        // === CANVAS + LAYERS ===
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2C2C2C))) {
                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> canvasEngine.startStroke(StrokePoint(offset.x, offset.y, 1f)) },
                        onDrag = { change, _ -> canvasEngine.addPointToStroke(StrokePoint(change.position.x, change.position.y, 1f)) },
                        onDragEnd = { canvasEngine.endStroke() }
                    )
                }) {
                    layers.forEach { layer ->
                        if (!layer.isVisible) return@forEach
                        (strokesByLayer[layer.id] ?: emptyList()).forEach { stroke -> drawStroke(stroke, layer.opacity) }
                    }
                    if (currentStroke.size > 1) {
                        val path = Path().apply {
                            moveTo(currentStroke[0].x, currentStroke[0].y)
                            for (i in 1 until currentStroke.size) lineTo(currentStroke[i].x, currentStroke[i].y)
                        }
                        val color = if (toolMode == ToolMode.ERASE) Color.Gray.copy(alpha = 0.4f) else Color(currentColor)
                        drawPath(path, color, style = Stroke(currentSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }

            // Layers panel
            Column(modifier = Modifier.width(130.dp).fillMaxHeight().background(Color(0xFF1A1A1A)).padding(6.dp)) {
                Text("Layers", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                layers.asReversed().forEach { layer ->
                    val active = layer.id == activeLayerId
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp))
                        .background(if (active) Color(0xFF3A2A5A) else Color.Transparent)
                        .clickable { canvasEngine.setActiveLayer(layer.id) }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                            .background(if (layer.isVisible) Color(0xFF03DAC6) else Color.Gray)
                            .clickable { canvasEngine.toggleLayerVisibility(layer.id) })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(layer.name, color = if (active) Color.White else Color.LightGray, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFF333333))
                    .clickable { canvasEngine.addLayer() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("+ Layer", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // === TIMELINE (Phase 2) ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF111111))
        ) {
            // Timeline controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Mode buttons
                    TimelineMode.values().forEach { mode ->
                        val selected = timelineMode == mode
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (selected) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { timeline.setMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${currentFrame} / ${durationFrames}", color = Color.LightGray, fontSize = 11.sp)
                    Text("%.0f fps".format(fps), color = Color.Gray, fontSize = 10.sp)

                    ToolButton(if (isPlaying) "Pause" else "Play") { timeline.togglePlay() }
                    ToolButton("|◀") { timeline.previousFrame() }
                    ToolButton("▶|") { timeline.nextFrame() }
                }
            }

            // Tracks + Playhead area
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                // Simple track representation
                Column {
                    tracks.forEachIndexed { index, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(track.name, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.width(70.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF2A2A2A))
                            )
                        }
                    }
                }

                // Playhead line
                val playheadFraction = if (durationFrames > 0) currentFrame.toFloat() / durationFrames else 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val x = 70.dp.toPx() + (size.width - 70.dp.toPx()) * playheadFraction
                    drawLine(
                        color = Color(0xFFFF5252),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2f
                    )
                }

                // Click to seek
                Box(modifier = Modifier.fillMaxSize().padding(start = 70.dp).pointerInput(durationFrames) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        timeline.seekTo((fraction * durationFrames).toInt())
                    }
                })
            }
        }
    }
}

@Composable
private fun ToolButton(text: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(5.dp))
        .background(when {
            selected -> Color(0xFF7C4DFF)
            enabled -> Color(0xFF333333)
            else -> Color(0xFF222222)
        }).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 11.sp)
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
