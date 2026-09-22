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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.engine.ToolMode
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.delay

@Composable
fun WorkspaceScreen() {
    val flipbook = remember { FlipbookBitmapEngine(width = 1920, height = 1080) }
    val timeline = remember { TimelineEngine() }

    val frames by flipbook.frames.collectAsState()
    val currentIndex by flipbook.currentIndex.collectAsState()
    val currentPath by flipbook.currentPath.collectAsState()
    val toolMode by flipbook.toolMode.collectAsState()
    val brushSize by flipbook.brushSize.collectAsState()
    val brushColor by flipbook.brushColor.collectAsState()
    val canUndo by flipbook.canUndo.collectAsState()
    val canRedo by flipbook.canRedo.collectAsState()
    val onionEnabled by flipbook.onionEnabled.collectAsState()

    val isPlaying by timeline.isPlaying.collectAsState()
    val timelineMode by timeline.mode.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Playback advances flipbook frames
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay((1000f / timeline.fps.value).toLong().coerceAtLeast(16))
            timeline.tick()
            // Sync flipbook to timeline frame (loop)
            val target = timeline.currentFrame.value % frames.size.coerceAtLeast(1)
            flipbook.setCurrentFrame(target)
        }
    }

    val colors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFF9800,
        0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ProAnimator", color = Color(0xFFBB86FC), fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToolButton("Draw", selected = toolMode == ToolMode.DRAW) {
                    flipbook.setToolMode(ToolMode.DRAW)
                }
                ToolButton("Eraser", selected = toolMode == ToolMode.ERASE) {
                    flipbook.setToolMode(ToolMode.ERASE)
                }
                ToolButton("Undo", enabled = canUndo) { flipbook.undo() }
                ToolButton("Redo", enabled = canRedo) { flipbook.redo() }
                ToolButton("Clear") { flipbook.clearCurrentFrame() }
            }
        }

        statusMessage?.let {
            Text(it, color = Color(0xFF03DAC6), fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 10.dp, vertical = 2.dp))
            LaunchedEffect(it) { delay(1400); statusMessage = null }
        }

        // TOOLS
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { c ->
                            val selected = brushColor == c
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(c))
                                .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                                .clickable { flipbook.setBrushColor(c) })
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (toolMode == ToolMode.ERASE) "Eraser" else "Size", color = Color.LightGray, fontSize = 10.sp)
                Slider(
                    value = brushSize,
                    onValueChange = { flipbook.setBrushSize(it) },
                    valueRange = 1f..80f,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFBB86FC), activeTrackColor = Color(0xFF7C4DFF))
                )
                Text("${brushSize.toInt()}", color = Color.White, fontSize = 10.sp)
            }
        }

        // CANVAS
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF2C2C2C))) {

            // Checkerboard
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = 16f
                var y = 0f
                var row = 0
                while (y < size.height) {
                    var x = 0f
                    var col = 0
                    while (x < size.width) {
                        drawRect(
                            color = if ((row + col) % 2 == 0) Color(0xFF3A3A3A) else Color(0xFF2E2E2E),
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(cell, cell)
                        )
                        x += cell
                        col++
                    }
                    y += cell
                    row++
                }
            }

            // Bitmap canvas with offscreen compositing (required for Clear)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(toolMode) {
                        detectDragGestures(
                            onDragStart = { o -> flipbook.startStroke(StrokePoint(o.x, o.y, 1f)) },
                            onDrag = { c, _ -> flipbook.addPoint(StrokePoint(c.position.x, c.position.y, 1f)) },
                            onDragEnd = { flipbook.endStroke() }
                        )
                    }
            ) {
                // Onion skin
                flipbook.getOnionLayers().forEach { layer ->
                    drawImage(
                        image = layer.bitmap,
                        alpha = layer.alpha,
                        colorFilter = ColorFilter.tint(layer.tint, androidx.compose.ui.graphics.BlendMode.SrcAtop)
                    )
                }

                // Current frame bitmap
                frames.getOrNull(currentIndex)?.let { frame ->
                    drawImage(image = frame.bitmap)
                }

                // Live stroke preview
                if (currentPath.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPath[0].x, currentPath[0].y)
                        for (i in 1 until currentPath.size) lineTo(currentPath[i].x, currentPath[i].y)
                    }
                    if (toolMode == ToolMode.ERASE) {
                        drawPath(path, Color.Gray.copy(0.45f),
                            style = Stroke(brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    } else {
                        drawPath(path, Color(brushColor),
                            style = Stroke(brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }

            // Overlay info
            Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Text("Frame ${currentIndex + 1} / ${frames.size}", color = Color.White.copy(0.85f), fontSize = 12.sp)
                Text(
                    if (toolMode == ToolMode.ERASE) "Eraser (true Clear)" else "Draw",
                    color = Color.White.copy(0.6f), fontSize = 10.sp
                )
            }

            Text(
                text = if (onionEnabled) "Onion ON" else "Onion OFF",
                color = if (onionEnabled) Color(0xFF03DAC6) else Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .clickable { flipbook.setOnionEnabled(!onionEnabled) }
            )
        }

        // BOTTOM: Flipbook controls + timeline modes
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))) {

            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 6.dp),
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
                ToolButton(if (isPlaying) "||" else ">") { timeline.togglePlay() }
            }

            // Frame strip + controls
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolButton("|◀") { flipbook.previousFrame() }
                ToolButton("▶|") { flipbook.nextFrame() }
                ToolButton("+F") { flipbook.addFrame(); statusMessage = "Frame added" }
                ToolButton("Dup") { flipbook.duplicateCurrentFrame(); statusMessage = "Duplicated" }
                ToolButton("Del") { flipbook.deleteCurrentFrame() }

                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    frames.forEachIndexed { index, _ ->
                        val isCurrent = index == currentIndex
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isCurrent) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                                .clickable { flipbook.setCurrentFrame(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
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
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    selected -> Color(0xFF7C4DFF)
                    enabled -> Color(0xFF333333)
                    else -> Color(0xFF222222)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 10.sp)
    }
}
