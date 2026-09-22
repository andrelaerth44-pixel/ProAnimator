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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.engine.LayerBitmapEngine
import com.proanimator.core.engine.ToolMode
import com.proanimator.core.timeline.FlipbookEngine
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.delay

@Composable
fun WorkspaceScreen() {
    val context = LocalContext.current

    // New bitmap-based engine (Phase 3)
    val bitmapEngine = remember { LayerBitmapEngine(width = 1920, height = 1080) }
    val timeline = remember { TimelineEngine() }
    val flipbookEngine = remember { FlipbookEngine() }

    val layers by bitmapEngine.layers.collectAsState()
    val activeLayerId by bitmapEngine.activeLayerId.collectAsState()
    val currentPath by bitmapEngine.currentPath.collectAsState()
    val toolMode by bitmapEngine.toolMode.collectAsState()
    val brushSize by bitmapEngine.brushSize.collectAsState()
    val brushColor by bitmapEngine.brushColor.collectAsState()
    val canUndo by bitmapEngine.canUndo.collectAsState()
    val canRedo by bitmapEngine.canRedo.collectAsState()

    val currentFrame by timeline.currentFrame.collectAsState()
    val isPlaying by timeline.isPlaying.collectAsState()
    val timelineMode by timeline.mode.collectAsState()
    val isRecording by timeline.isRecording.collectAsState()

    val flipbook by flipbookEngine.flipbook.collectAsState()
    val flipbookFrameIndex by flipbookEngine.currentFrameIndex.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay((1000f / timeline.fps.value).toLong().coerceAtLeast(16))
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
                    bitmapEngine.setToolMode(ToolMode.DRAW)
                }
                ToolButton("Eraser", selected = toolMode == ToolMode.ERASE) {
                    bitmapEngine.setToolMode(ToolMode.ERASE)
                }
                ToolButton("Undo", enabled = canUndo) { bitmapEngine.undo() }
                ToolButton("Redo", enabled = canRedo) { bitmapEngine.redo() }
                ToolButton("Clear") { bitmapEngine.clearActiveLayer() }
            }
        }

        statusMessage?.let {
            Text(
                it,
                color = Color(0xFF03DAC6),
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
            LaunchedEffect(it) {
                delay(1500)
                statusMessage = null
            }
        }

        // TOOLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (toolMode == ToolMode.DRAW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { c ->
                            val selected = brushColor == c
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) Color.White else Color.Gray,
                                        CircleShape
                                    )
                                    .clickable { bitmapEngine.setBrushColor(c) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    if (toolMode == ToolMode.ERASE) "Eraser" else "Size",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
                Slider(
                    value = brushSize,
                    onValueChange = { bitmapEngine.setBrushSize(it) },
                    valueRange = 1f..80f,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFBB86FC),
                        activeTrackColor = Color(0xFF7C4DFF)
                    )
                )
                Text("${brushSize.toInt()}", color = Color.White, fontSize = 10.sp)
            }
        }

        // CANVAS + LAYERS
        Row(modifier = Modifier.weight(1f)) {

            // Main canvas with ImageBitmap layers
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF2C2C2C))
            ) {
                // Checkerboard to show transparency (important for eraser)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cell = 16f
                    var y = 0f
                    var row = 0
                    while (y < size.height) {
                        var x = 0f
                        var col = 0
                        while (x < size.width) {
                            val dark = (row + col) % 2 == 0
                            drawRect(
                                color = if (dark) Color(0xFF3A3A3A) else Color(0xFF2E2E2E),
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

                // Layer bitmaps + live stroke
                // graphicsLayer with Offscreen is critical for BlendMode.Clear to work correctly
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .pointerInput(toolMode) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    bitmapEngine.startStroke(StrokePoint(offset.x, offset.y, 1f))
                                },
                                onDrag = { change, _ ->
                                    bitmapEngine.addPoint(
                                        StrokePoint(change.position.x, change.position.y, 1f)
                                    )
                                },
                                onDragEnd = {
                                    bitmapEngine.endStroke()
                                }
                            )
                        }
                ) {
                    // Draw all visible layer bitmaps
                    layers.forEach { layer ->
                        if (!layer.isVisible) return@forEach
                        drawImage(
                            image = layer.bitmap,
                            alpha = layer.opacity
                        )
                    }

                    // Live stroke preview
                    if (currentPath.size > 1) {
                        val path = Path().apply {
                            moveTo(currentPath[0].x, currentPath[0].y)
                            for (i in 1 until currentPath.size) {
                                lineTo(currentPath[i].x, currentPath[i].y)
                            }
                        }

                        if (toolMode == ToolMode.ERASE) {
                            // Preview eraser as semi-transparent dark stroke
                            drawPath(
                                path = path,
                                color = Color.Gray.copy(alpha = 0.5f),
                                style = Stroke(
                                    width = brushSize,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else {
                            drawPath(
                                path = path,
                                color = Color(brushColor),
                                style = Stroke(
                                    width = brushSize,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // Info
                Text(
                    text = if (toolMode == ToolMode.ERASE) "Eraser (true Clear)" else "Draw",
                    color = Color.White.copy(0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            // Layers panel
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1A1A))
                    .padding(6.dp)
            ) {
                Text("Layers", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                layers.asReversed().forEach { layer ->
                    val active = layer.id == activeLayerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) Color(0xFF3A2A5A) else Color.Transparent)
                            .clickable { bitmapEngine.setActiveLayer(layer.id) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (layer.isVisible) Color(0xFF03DAC6) else Color.Gray)
                                .clickable { bitmapEngine.toggleLayerVisibility(layer.id) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            layer.name,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF333333))
                        .clickable { bitmapEngine.addLayer() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Layer", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // Bottom bar (timeline placeholder + modes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF111111))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimelineMode.values().forEach { mode ->
                    val selected = timelineMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selected) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { timeline.setMode(mode) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(mode.name.take(3), color = Color.White, fontSize = 9.sp)
                    }
                }
            }

            Text(
                "ImageBitmap + True Eraser", 
                color = Color(0xFF03DAC6), 
                fontSize = 10.sp
            )
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
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 11.sp)
    }
}
