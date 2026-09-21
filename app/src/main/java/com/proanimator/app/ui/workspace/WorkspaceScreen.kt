package com.proanimator.app.ui.workspace

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
import com.proanimator.domain.model.EasingType
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

    // Demo property for keyframe testing (opacity of a virtual object)
    val demoOpacity by remember {
        derivedStateOf { timeline.getPropertyValue("opacity", currentFrame) }
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }

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

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ProAnimator", color = Color(0xFFBB86FC), fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToolButton("Draw", selected = toolMode == ToolMode.DRAW) { canvasEngine.setToolMode(ToolMode.DRAW) }
                ToolButton("Eraser", selected = toolMode == ToolMode.ERASE) { canvasEngine.setToolMode(ToolMode.ERASE) }
                ToolButton("Undo", enabled = canUndo) { canvasEngine.undo() }
                ToolButton("Redo", enabled = canRedo) { canvasEngine.redo() }
                ToolButton("Save") {
                    try {
                        File(context.filesDir, "project.json").writeText(ProjectSerializer.toJson(canvasEngine))
                        statusMessage = "Saved"
                    } catch (e: Exception) { statusMessage = "Error" }
                }
                ToolButton("Load") {
                    try {
                        val f = File(context.filesDir, "project.json")
                        if (f.exists()) {
                            ProjectSerializer.fromJson(f.readText(), canvasEngine)
                            statusMessage = "Loaded"
                        } else statusMessage = "No save"
                    } catch (e: Exception) { statusMessage = "Error" }
                }
            }
        }

        statusMessage?.let {
            Text(it, color = Color(0xFF03DAC6), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 10.dp, vertical = 2.dp))
            LaunchedEffect(it) { delay(1600); statusMessage = null }
        }

        // TOOLS
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(vertical = 5.dp)) {
            if (toolMode == ToolMode.DRAW) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DefaultBrushes.all.forEach { brush ->
                        val sel = currentBrushId == brush.id
                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF333333))
                            .clickable { canvasEngine.setBrush(brush.id); canvasEngine.setSize(brush.defaultSize) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)) {
                            Text(brush.name, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { c ->
                            val sel = currentColor == c
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(c))
                                .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color.Gray, CircleShape)
                                .clickable { canvasEngine.setColor(c) })
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Size", color = Color.LightGray, fontSize = 10.sp)
                Slider(value = currentSize, onValueChange = { canvasEngine.setSize(it) }, valueRange = 1f..70f,
                    modifier = Modifier.width(90.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFBB86FC), activeTrackColor = Color(0xFF7C4DFF)))
            }
        }

        // CANVAS + LAYERS
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2C2C2C))) {
                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o -> canvasEngine.startStroke(StrokePoint(o.x, o.y, 1f)) },
                        onDrag = { c, _ -> canvasEngine.addPointToStroke(StrokePoint(c.position.x, c.position.y, 1f)) },
                        onDragEnd = { canvasEngine.endStroke() }
                    )
                }) {
                    layers.forEach { layer ->
                        if (!layer.isVisible) return@forEach
                        (strokesByLayer[layer.id] ?: emptyList()).forEach { s -> drawStroke(s, layer.opacity) }
                    }
                    if (currentStroke.size > 1) {
                        val path = Path().apply {
                            moveTo(currentStroke[0].x, currentStroke[0].y)
                            for (i in 1 until currentStroke.size) lineTo(currentStroke[i].x, currentStroke[i].y)
                        }
                        val col = if (toolMode == ToolMode.ERASE) Color.Gray.copy(0.4f) else Color(currentColor)
                        drawPath(path, col, style = Stroke(currentSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                // Demo keyframe indicator
                if (timelineMode == TimelineMode.KEYFRAME) {
                    Text(
                        text = "Opacity demo: ${(demoOpacity * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                }
            }

            Column(modifier = Modifier.width(120.dp).fillMaxHeight().background(Color(0xFF1A1A1A)).padding(5.dp)) {
                Text("Layers", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                layers.asReversed().forEach { layer ->
                    val active = layer.id == activeLayerId
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        .background(if (active) Color(0xFF3A2A5A) else Color.Transparent)
                        .clickable { canvasEngine.setActiveLayer(layer.id) }.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape)
                            .background(if (layer.isVisible) Color(0xFF03DAC6) else Color.Gray)
                            .clickable { canvasEngine.toggleLayerVisibility(layer.id) })
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(layer.name, color = if (active) Color.White else Color.LightGray, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF333333))
                    .clickable { canvasEngine.addLayer() }.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                    Text("+ Layer", color = Color.White, fontSize = 10.sp)
                }
            }
        }

        // === TIMELINE ===
        Column(modifier = Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF111111))) {

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimelineMode.values().forEach { mode ->
                        val sel = timelineMode == mode
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { timeline.setMode(mode) }
                            .padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(mode.name.take(3), color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$currentFrame / $durationFrames", color = Color.LightGray, fontSize = 10.sp)

                    // Keyframe quick actions (only in Keyframe mode)
                    if (timelineMode == TimelineMode.KEYFRAME) {
                        ToolButton("+KF") {
                            timeline.addOrUpdateKeyframe("opacity", currentFrame, 1f, EasingType.EASE_IN_OUT)
                            statusMessage = "Keyframe added at $currentFrame"
                        }
                        ToolButton("KF 0") {
                            timeline.addOrUpdateKeyframe("opacity", currentFrame, 0f, EasingType.EASE_IN_OUT)
                            statusMessage = "Opacity 0 at $currentFrame"
                        }
                    }

                    ToolButton(if (isPlaying) "||" else ">") { timeline.togglePlay() }
                    ToolButton("<") { timeline.previousFrame() }
                    ToolButton(">") { timeline.nextFrame() }
                }
            }

            // Tracks area
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 3.dp)) {
                Column {
                    tracks.forEach { track ->
                        Row(modifier = Modifier.fillMaxWidth().height(26.dp).padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(track.name, color = Color.LightGray, fontSize = 9.sp, modifier = Modifier.width(60.dp))
                            Box(modifier = Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A2A2A)))
                        }
                    }

                    // Demo property track
                    if (timelineMode == TimelineMode.KEYFRAME) {
                        Row(modifier = Modifier.fillMaxWidth().height(26.dp).padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("opacity", color = Color(0xFF03DAC6), fontSize = 9.sp, modifier = Modifier.width(60.dp))
                            Box(modifier = Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1E3A3A)))
                        }
                    }
                }

                // Playhead
                val fraction = if (durationFrames > 0) currentFrame.toFloat() / durationFrames else 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val x = 60.dp.toPx() + (size.width - 60.dp.toPx()) * fraction
                    drawLine(Color(0xFFFF5252), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                }

                Box(modifier = Modifier.fillMaxSize().padding(start = 60.dp).pointerInput(durationFrames) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        timeline.seekTo((f * durationFrames).toInt())
                    }
                })
            }
        }
    }
}

@Composable
private fun ToolButton(text: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
        .background(when {
            selected -> Color(0xFF7C4DFF)
            enabled -> Color(0xFF333333)
            else -> Color(0xFF222222)
        }).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 7.dp, vertical = 4.dp)) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 10.sp)
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
