package com.proanimator.app.ui.workspace

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
import com.proanimator.core.timeline.FlipbookEngine
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode
import com.proanimator.domain.model.EasingType
import com.proanimator.domain.model.PropertyType
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun WorkspaceScreen() {
    val context = LocalContext.current
    val canvasEngine = remember { CanvasEngine(width = 1920, height = 1080) }
    val timeline = remember { TimelineEngine() }
    val flipbookEngine = remember { FlipbookEngine() }

    val currentStroke by canvasEngine.currentStroke.collectAsState()
    val currentBrushId by canvasEngine.currentBrushId.collectAsState()
    val currentSize by canvasEngine.currentSize.collectAsState()
    val currentColor by canvasEngine.currentColor.collectAsState()
    val toolMode by canvasEngine.toolMode.collectAsState()
    val canUndo by canvasEngine.canUndo.collectAsState()
    val canRedo by canvasEngine.canRedo.collectAsState()

    val currentFrame by timeline.currentFrame.collectAsState()
    val isPlaying by timeline.isPlaying.collectAsState()
    val timelineMode by timeline.mode.collectAsState()
    val isRecording by timeline.isRecording.collectAsState()

    val flipbook by flipbookEngine.flipbook.collectAsState()
    val flipbookFrameIndex by flipbookEngine.currentFrameIndex.collectAsState()

    // Live property values
    val opacity by remember { derivedStateOf { timeline.getPropertyValue(PropertyType.OPACITY, currentFrame) } }
    val posX by remember { derivedStateOf { timeline.getPropertyValue(PropertyType.POSITION_X, currentFrame) } }
    val posY by remember { derivedStateOf { timeline.getPropertyValue(PropertyType.POSITION_Y, currentFrame) } }
    val scale by remember { derivedStateOf { timeline.getPropertyValue(PropertyType.SCALE, currentFrame) } }
    val rotation by remember { derivedStateOf { timeline.getPropertyValue(PropertyType.ROTATION, currentFrame) } }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay((1000f / timeline.fps.value).toLong().coerceAtLeast(16))
            timeline.tick()
        }
    }

    val colors = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63)

    Column(modifier = Modifier.fillMaxSize()) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ProAnimator", color = Color(0xFFBB86FC), fontSize = 14.sp, fontWeight = FontWeight.Bold)

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
            }
        }

        statusMessage?.let {
            Text(it, color = Color(0xFF03DAC6), fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 10.dp, vertical = 2.dp))
            LaunchedEffect(it) { delay(1400); statusMessage = null }
        }

        // TOOLS
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(vertical = 4.dp)) {
            if (toolMode == ToolMode.DRAW) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DefaultBrushes.all.forEach { brush ->
                        val sel = currentBrushId == brush.id
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF333333))
                            .clickable { canvasEngine.setBrush(brush.id); canvasEngine.setSize(brush.defaultSize) }
                            .padding(horizontal = 7.dp, vertical = 4.dp)) {
                            Text(brush.name, color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        colors.forEach { c ->
                            val sel = currentColor == c
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(c))
                                .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color.Gray, CircleShape)
                                .clickable { canvasEngine.setColor(c) })
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Size", color = Color.LightGray, fontSize = 9.sp)
                Slider(value = currentSize, onValueChange = { canvasEngine.setSize(it) }, valueRange = 1f..50f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFBB86FC), activeTrackColor = Color(0xFF7C4DFF)))
            }
        }

        // CANVAS
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF2C2C2C))) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o -> canvasEngine.startStroke(StrokePoint(o.x, o.y, 1f)) },
                    onDrag = { c, _ -> canvasEngine.addPointToStroke(StrokePoint(c.position.x, c.position.y, 1f)) },
                    onDragEnd = {
                        val stroke = canvasEngine.endStroke()
                        stroke?.let { flipbookEngine.addStrokeToCurrentFrame(it) }
                    }
                )
            }) {
                // Onion Skin with classic colors
                flipbookEngine.getOnionSkinLayers().forEach { layer ->
                    layer.strokes.forEach { stroke ->
                        drawStrokeTinted(stroke, layer.color, layer.opacity)
                    }
                }

                // Current frame
                flipbookEngine.currentFrame?.strokes?.forEach { stroke ->
                    drawStroke(stroke, 1f)
                }

                // Live stroke
                if (currentStroke.size > 1) {
                    val path = Path().apply {
                        moveTo(currentStroke[0].x, currentStroke[0].y)
                        for (i in 1 until currentStroke.size) lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                    val col = if (toolMode == ToolMode.ERASE) Color.Gray.copy(0.4f) else Color(currentColor)
                    drawPath(path, col, style = Stroke(currentSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // Info overlays
            Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Text("Frame ${flipbookFrameIndex + 1}/${flipbook.frameCount}", color = Color.White.copy(0.85f), fontSize = 12.sp)
                if (timelineMode == TimelineMode.KEYFRAME || timelineMode == TimelineMode.PERFORM) {
                    Text("Op:${(opacity*100).toInt()}%  S:${String.format("%.2f", scale)}  R:${rotation.toInt()}°", color = Color(0xFF03DAC6), fontSize = 10.sp)
                }
            }

            Text(
                text = if (flipbook.onionSkinEnabled) "Onion ON" else "Onion OFF",
                color = if (flipbook.onionSkinEnabled) Color(0xFF03DAC6) else Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .clickable { flipbookEngine.setOnionSkinEnabled(!flipbook.onionSkinEnabled) }
            )

            if (isRecording) {
                Text("● REC", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
            }
        }

        // CONTROLS
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))) {

            // Mode + playback + keyframe actions
            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    TimelineMode.values().forEach { mode ->
                        val sel = timelineMode == mode
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { timeline.setMode(mode) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text(mode.name.take(3), color = Color.White, fontSize = 9.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (timelineMode == TimelineMode.KEYFRAME) {
                        ToolButton("+Op") { timeline.addOrUpdateKeyframe(PropertyType.OPACITY, currentFrame, 1f); statusMessage = "Opacity KF" }
                        ToolButton("Op0") { timeline.addOrUpdateKeyframe(PropertyType.OPACITY, currentFrame, 0f) }
                        ToolButton("+Sc") { timeline.addOrUpdateKeyframe(PropertyType.SCALE, currentFrame, 1.5f); statusMessage = "Scale KF" }
                        ToolButton("+Rot") { timeline.addOrUpdateKeyframe(PropertyType.ROTATION, currentFrame, 45f); statusMessage = "Rot KF" }
                    }
                    if (timelineMode == TimelineMode.PERFORM) {
                        ToolButton(if (isRecording) "Stop" else "Rec", selected = isRecording) { timeline.toggleRecording() }
                    }
                    ToolButton(if (isPlaying) "||" else ">") { timeline.togglePlay() }
                }
            }

            // Flipbook controls + frame strip
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolButton("|◀") { flipbookEngine.previousFrame() }
                ToolButton("▶|") { flipbookEngine.nextFrame() }
                ToolButton("+F") { flipbookEngine.addFrame() }
                ToolButton("Dup") { flipbookEngine.duplicateCurrentFrame() }
                ToolButton("Del") { flipbookEngine.deleteCurrentFrame() }

                // Frame strip
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    flipbook.frames.forEachIndexed { index, _ ->
                        val isCurrent = index == flipbookFrameIndex
                        Box(modifier = Modifier.width(28.dp).height(22.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (isCurrent) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { flipbookEngine.setCurrentFrame(index) },
                            contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(text: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
        .background(when {
            selected -> Color(0xFF7C4DFF)
            enabled -> Color(0xFF333333)
            else -> Color(0xFF222222)
        }).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 9.sp)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: Stroke, opacity: Float) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) lineTo(stroke.points[i].x, stroke.points[i].y)
    }
    if (stroke.brushId == "eraser") {
        drawPath(path, Color.DarkGray.copy(alpha = 0.3f * opacity), style = Stroke(stroke.size, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
        drawPath(path, Color(stroke.color).copy(alpha = opacity * stroke.opacity), style = Stroke(stroke.size, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeTinted(stroke: Stroke, tint: Long, opacity: Float) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) lineTo(stroke.points[i].x, stroke.points[i].y)
    }
    drawPath(path, Color(tint).copy(alpha = opacity), style = Stroke(stroke.size, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
