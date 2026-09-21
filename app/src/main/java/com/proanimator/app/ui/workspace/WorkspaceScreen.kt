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
import com.proanimator.core.timeline.FlipbookEngine
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
    val flipbookEngine = remember { FlipbookEngine() }

    val layers by canvasEngine.layers.collectAsState()
    val strokesByLayer by canvasEngine.strokesByLayer.collectAsState()
    val currentStroke by canvasEngine.currentStroke.collectAsState()
    val currentBrushId by canvasEngine.currentBrushId.collectAsState()
    val currentSize by canvasEngine.currentSize.collectAsState()
    val currentColor by canvasEngine.currentColor.collectAsState()
    val toolMode by canvasEngine.toolMode.collectAsState()
    val canUndo by canvasEngine.canUndo.collectAsState()
    val canRedo by canvasEngine.canRedo.collectAsState()

    val currentFrame by timeline.currentFrame.collectAsState()
    val isPlaying by timeline.isPlaying.collectAsState()
    val durationFrames by timeline.durationFrames.collectAsState()
    val timelineMode by timeline.mode.collectAsState()

    val flipbook by flipbookEngine.flipbook.collectAsState()
    val flipbookFrameIndex by flipbookEngine.currentFrameIndex.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // When in Flipbook-style workflow we sync timeline frame with flipbook frame
    LaunchedEffect(timelineMode) {
        if (timelineMode == TimelineMode.COMPOSE) {
            // keep them independent for now
        }
    }

    LaunchedEffect(isPlaying, timeline.fps.value) {
        while (isPlaying) {
            delay((1000f / timeline.fps.value).toLong().coerceAtLeast(16))
            timeline.tick()
            // Also advance flipbook when playing
            if (timelineMode == TimelineMode.COMPOSE) {
                // optional: flipbookEngine.setCurrentFrame(timeline.currentFrame.value % flipbook.frameCount)
            }
        }
    }

    val colors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFF9800,
        0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp),
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
            }
        }

        statusMessage?.let {
            Text(it, color = Color(0xFF03DAC6), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 10.dp, vertical = 2.dp))
            LaunchedEffect(it) { delay(1500); statusMessage = null }
        }

        // TOOLS BAR
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(vertical = 5.dp)) {
            if (toolMode == ToolMode.DRAW) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DefaultBrushes.all.forEach { brush ->
                        val sel = currentBrushId == brush.id
                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF333333))
                            .clickable { canvasEngine.setBrush(brush.id); canvasEngine.setSize(brush.defaultSize) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(brush.name, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { c ->
                            val sel = currentColor == c
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(c))
                                .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color.Gray, CircleShape)
                                .clickable { canvasEngine.setColor(c) })
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Size", color = Color.LightGray, fontSize = 10.sp)
                Slider(value = currentSize, onValueChange = { canvasEngine.setSize(it) }, valueRange = 1f..60f,
                    modifier = Modifier.width(90.dp),
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
                        // Also add to current flipbook frame
                        stroke?.let { flipbookEngine.addStrokeToCurrentFrame(it) }
                    }
                )
            }) {
                // 1. Onion Skin (previous + next frames)
                flipbookEngine.getOnionSkinStrokes().forEach { (strokes, opacity) ->
                    strokes.forEach { stroke ->
                        drawStroke(stroke, opacity)
                    }
                }

                // 2. Current flipbook frame strokes
                flipbookEngine.currentFrame?.strokes?.forEach { stroke ->
                    drawStroke(stroke, 1f)
                }

                // 3. Live stroke being drawn
                if (currentStroke.size > 1) {
                    val path = Path().apply {
                        moveTo(currentStroke[0].x, currentStroke[0].y)
                        for (i in 1 until currentStroke.size) lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                    val col = if (toolMode == ToolMode.ERASE) Color.Gray.copy(0.4f) else Color(currentColor)
                    drawPath(path, col, style = Stroke(currentSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // Frame indicator
            Text(
                text = "Frame ${flipbookFrameIndex + 1} / ${flipbook.frameCount}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            )

            // Onion skin toggle
            Text(
                text = if (flipbook.onionSkinEnabled) "Onion ON" else "Onion OFF",
                color = if (flipbook.onionSkinEnabled) Color(0xFF03DAC6) else Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clickable { flipbookEngine.setOnionSkinEnabled(!flipbook.onionSkinEnabled) }
            )
        }

        // === FLIPBOOK + TIMELINE CONTROLS ===
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))) {

            // Flipbook controls
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ToolButton("|◀") { flipbookEngine.previousFrame() }
                    ToolButton("▶|") { flipbookEngine.nextFrame() }
                    ToolButton("+Frame") { flipbookEngine.addFrame(); statusMessage = "Frame added" }
                    ToolButton("Dup") { flipbookEngine.duplicateCurrentFrame(); statusMessage = "Duplicated" }
                    ToolButton("Del") { flipbookEngine.deleteCurrentFrame() }
                    ToolButton("Clear") { flipbookEngine.clearCurrentFrame() }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TimelineMode.values().forEach { mode ->
                        val sel = timelineMode == mode
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { timeline.setMode(mode) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text(mode.name.take(3), color = Color.White, fontSize = 9.sp)
                        }
                    }
                    ToolButton(if (isPlaying) "||" else ">") { timeline.togglePlay() }
                }
            }

            // Simple frame strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                flipbook.frames.forEachIndexed { index, _ ->
                    val isCurrent = index == flipbookFrameIndex
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrent) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .border(if (isCurrent) 2.dp else 0.dp, Color.White, RoundedCornerShape(4.dp))
                            .clickable { flipbookEngine.setCurrentFrame(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White, fontSize = 11.sp)
                    }
                }
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
