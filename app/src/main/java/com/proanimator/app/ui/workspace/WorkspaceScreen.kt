package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.brushes.BrushLibrary
import com.proanimator.core.engine.CanvasViewport
import com.proanimator.core.engine.detectPressureStroke
import com.proanimator.core.export.ExportEngine
import com.proanimator.core.export.ProjectSerializer
import com.proanimator.core.ink.InkBridge
import com.proanimator.core.lottie.LottieFrameImporter
import com.proanimator.core.timeline.AnimProperty
import com.proanimator.core.timeline.EasingType
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.PerformEngine
import com.proanimator.core.timeline.TimelineEngine
import com.proanimator.core.timeline.TimelineMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkspaceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val flipbook = remember { FlipbookBitmapEngine(width = 1920, height = 1080) }
    val timeline = remember { TimelineEngine() }
    val perform = remember { PerformEngine() }
    val exportEngine = remember { ExportEngine(context) }
    val serializer = remember { ProjectSerializer(context) }
    val lottieImporter = remember { LottieFrameImporter(context) }
    val viewport = remember { CanvasViewport(1920f, 1080f) }

    var viewScale by remember { mutableFloatStateOf(1f) }
    var viewOffset by remember { mutableStateOf(Offset.Zero) }

    val frames by flipbook.frames.collectAsState()
    val currentIndex by flipbook.currentIndex.collectAsState()
    val currentPath by flipbook.currentPath.collectAsState()
    val activeBrush by flipbook.brushEngine.activeBrush.collectAsState()
    val brushColor by flipbook.brushEngine.color.collectAsState()
    val sizeMul by flipbook.brushEngine.sizeMul.collectAsState()
    val canUndo by flipbook.canUndo.collectAsState()
    val canRedo by flipbook.canRedo.collectAsState()
    val onionEnabled by flipbook.onionEnabled.collectAsState()

    val isPlaying by timeline.isPlaying.collectAsState()
    val timelineMode by timeline.mode.collectAsState()
    val isRecording by perform.isRecording.collectAsState()
    val perfX by perform.posX.collectAsState()
    val perfY by perform.posY.collectAsState()
    val perfScale by perform.scale.collectAsState()
    val perfRot by perform.rotation.collectAsState()
    val perfOpacity by perform.opacity.collectAsState()
    val performRevision by perform.revision.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showProjects by remember { mutableStateOf(false) }
    var projectList by remember { mutableStateOf(serializer.listProjects()) }
    var showBezier by remember { mutableStateOf(false) }
    var selectedProp by remember { mutableStateOf(AnimProperty.POS_X) }
    var bezierX1 by remember { mutableFloatStateOf(0.42f) }
    var bezierY1 by remember { mutableFloatStateOf(0f) }
    var bezierX2 by remember { mutableFloatStateOf(0.58f) }
    var bezierY2 by remember { mutableFloatStateOf(1f) }
    var selectedEasing by remember { mutableStateOf(EasingType.BEZIER) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay((1000f / timeline.fps.value).toLong().coerceAtLeast(16))
            timeline.tick()
            val target = timeline.currentFrame.value % frames.size.coerceAtLeast(1)
            flipbook.setCurrentFrame(target)
            perform.evaluate(timeline.currentFrame.value.toFloat())
        }
    }

    val colors = listOf(
        0xFFFFFFFF, 0xFF000000, 0xFFFF5252, 0xFFFF9800,
        0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63
    )

    fun currentValueFor(prop: AnimProperty): Float = when (prop) {
        AnimProperty.POS_X -> perfX
        AnimProperty.POS_Y -> perfY
        AnimProperty.SCALE -> perfScale
        AnimProperty.ROTATION -> perfRot
        AnimProperty.OPACITY -> perfOpacity
    }

    fun addKf(prop: AnimProperty, frame: Int) {
        perform.addKeyframe(
            prop, frame, currentValueFor(prop), selectedEasing,
            bezierX1, bezierY1, bezierX2, bezierY2
        )
        statusMessage = "KF ${prop.name} @ F$frame"
    }

    fun doSave() {
        scope.launch {
            val meta = ProjectSerializer.buildMeta(
                activeBrush.id, onionEnabled, timelineMode.name, perform
            )
            serializer.save(
                ProjectSerializer.ProjectData(
                    flipbook.width, flipbook.height, timeline.fps.value,
                    currentIndex, flipbook.getAllBitmaps(), meta
                )
            ).onSuccess {
                statusMessage = "Saved ${it.name} (${perform.keyframeCount()} KFs)"
                projectList = serializer.listProjects()
            }.onFailure { statusMessage = "Save fail" }
        }
    }

    fun doLoad(file: java.io.File) {
        scope.launch {
            serializer.load(file).onSuccess { data ->
                flipbook.loadFrames(data.frames, data.currentFrameIndex)
                val extras = ProjectSerializer.applyMeta(data.meta, perform)
                flipbook.setOnionEnabled(extras.onionEnabled)
                BrushLibrary.ALL.find { it.id == extras.brushId }?.let { flipbook.setBrush(it) }
                try { timeline.setMode(TimelineMode.valueOf(extras.timelineMode)) } catch (_: Exception) {}
                perform.evaluate(data.currentFrameIndex.toFloat())
                statusMessage = "Loaded ${data.frames.size}f / ${perform.keyframeCount()} KF"
                showProjects = false
            }.onFailure { statusMessage = "Load fail" }
        }
    }

    fun doImportLottie() {
        scope.launch {
            statusMessage = "Import Lottie…"
            // Demo: try common asset names; user can place JSON in assets/
            val candidates = listOf("demo.json", "animation.json", "lottie.json")
            var done = false
            for (name in candidates) {
                val result = lottieImporter.importFromAssets(
                    name,
                    targetWidth = flipbook.width,
                    targetHeight = flipbook.height,
                    maxFrames = 60
                )
                result.onSuccess { imp ->
                    flipbook.loadFrames(imp.frames, 0)
                    statusMessage = "Lottie: ${imp.name} ${imp.frames.size} frames @${imp.fps}fps"
                    done = true
                }
                if (done) break
            }
            if (!done) {
                statusMessage = "Put .json in assets/ as demo.json (Lottie)"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ProAnimator", color = Color(0xFFBB86FC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ToolButton("Undo", enabled = canUndo) { flipbook.undo() }
                ToolButton("Redo", enabled = canRedo) { flipbook.redo() }
                ToolButton("Save") { doSave() }
                ToolButton("Load") {
                    projectList = serializer.listProjects()
                    showProjects = !showProjects
                }
                ToolButton("MP4") {
                    scope.launch {
                        exportEngine.exportMp4(
                            flipbook.getAllBitmaps(), flipbook.width, flipbook.height, timeline.fps.value
                        ).onSuccess { statusMessage = "MP4 ok" }
                    }
                }
                ToolButton("Lottie") { doImportLottie() }
                ToolButton("1:1") {
                    viewport.reset()
                    viewScale = 1f
                    viewOffset = Offset.Zero
                }
                ToolButton("Bezier", selected = showBezier) { showBezier = !showBezier }
                ToolButton(if (isRecording) "REC●" else "REC", selected = isRecording) {
                    perform.toggleRecording()
                }
            }
        }

        if (showProjects) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp)) {
                projectList.take(6).forEach { f ->
                    Text(f.name, color = Color(0xFF03DAC6), fontSize = 11.sp,
                        modifier = Modifier.clickable { doLoad(f) }.padding(4.dp))
                }
                Text("Close", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.clickable { showProjects = false })
            }
        }

        statusMessage?.let {
            Text(it, color = Color(0xFF03DAC6), fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp, vertical = 2.dp))
            LaunchedEffect(it) { delay(2800); statusMessage = null }
        }

        // Brush row
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(horizontal = 6.dp, vertical = 3.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrushLibrary.ALL.forEach { preset ->
                val sel = activeBrush.id == preset.id
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF333333))
                        .clickable { flipbook.setBrush(preset) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) { Text(preset.name, color = Color.White, fontSize = 9.sp) }
            }
            if (!activeBrush.isEraser) {
                colors.forEach { c ->
                    val sel = brushColor == c
                    Box(modifier = Modifier.size(13.dp).clip(CircleShape).background(Color(c))
                        .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color.Gray, CircleShape)
                        .clickable { flipbook.setBrushColor(c) })
                }
            }
            Slider(
                value = sizeMul, onValueChange = { flipbook.setSizeMultiplier(it) },
                valueRange = 0.5f..3f, modifier = Modifier.width(60.dp),
                colors = SliderDefaults.colors(thumbColor = Color(0xFFBB86FC), activeTrackColor = Color(0xFF7C4DFF))
            )
            Text("${(viewScale * 100).toInt()}%", color = Color.Gray, fontSize = 9.sp)
            Text(InkBridge.describe().take(12), color = Color(0xFF666666), fontSize = 8.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1C)).padding(horizontal = 6.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("Ease:", color = Color.Gray, fontSize = 9.sp)
            EasingType.entries.forEach { e ->
                val sel = selectedEasing == e
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(3.dp))
                        .background(if (sel) Color(0xFFFF9800) else Color(0xFF2A2A2A))
                        .clickable { selectedEasing = e }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) { Text(e.name.take(6), color = Color.White, fontSize = 8.sp) }
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2C2C2C))) {
                // Checkerboard (screen space)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cell = 16f
                    var y = 0f; var row = 0
                    while (y < size.height) {
                        var x = 0f; var col = 0
                        while (x < size.width) {
                            drawRect(
                                color = if ((row + col) % 2 == 0) Color(0xFF3A3A3A) else Color(0xFF2E2E2E),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(cell, cell)
                            )
                            x += cell; col++
                        }
                        y += cell; row++
                    }
                }

                // Drawing layer: zoom/pan via withTransform + dual pointer handlers
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        // Two-finger zoom/pan
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                viewport.applyZoomPan(centroid, pan, zoom)
                                viewScale = viewport.scale
                                viewOffset = viewport.offset
                            }
                        }
                        // One-finger / stylus pressure stroke
                        .pointerInput(isRecording, viewScale, viewOffset) {
                            if (isRecording) {
                                detectTransformGestures { _, pan, _, _ ->
                                    perform.recordDrag(currentIndex, pan.x / viewScale, pan.y / viewScale)
                                }
                            } else {
                                detectPressureStroke(
                                    onStart = { flipbook.startStroke(it) },
                                    onMove = { flipbook.addPoint(it) },
                                    onEnd = { flipbook.endStroke() },
                                    toCanvas = { screen ->
                                        // Inverse of view transform
                                        Offset(
                                            screen.x / viewScale - viewOffset.x,
                                            screen.y / viewScale - viewOffset.y
                                        )
                                    }
                                )
                            }
                        }
                ) {
                    withTransform({
                        scale(viewScale, viewScale)
                        translate(viewOffset.x, viewOffset.y)
                        // Perform transforms on content
                        translate(perfX, perfY)
                        scale(perfScale, perfScale)
                        rotate(perfRot)
                    }) {
                        flipbook.getOnionLayers().forEach { layer ->
                            drawImage(
                                image = layer.bitmap,
                                alpha = layer.alpha * perfOpacity.coerceIn(0.05f, 1f),
                                colorFilter = ColorFilter.tint(
                                    layer.tint,
                                    androidx.compose.ui.graphics.BlendMode.SrcAtop
                                )
                            )
                        }
                        frames.getOrNull(currentIndex)?.let {
                            drawImage(image = it.bitmap, alpha = perfOpacity.coerceIn(0.05f, 1f))
                        }
                        if (currentPath.size > 1 && !isRecording) {
                            val path = Path().apply {
                                moveTo(currentPath[0].x, currentPath[0].y)
                                for (i in 1 until currentPath.size) {
                                    lineTo(currentPath[i].x, currentPath[i].y)
                                }
                            }
                            val w = activeBrush.baseSize * sizeMul *
                                (0.4f + 0.6f * (currentPath.lastOrNull()?.pressure ?: 1f))
                            drawPath(
                                path,
                                if (activeBrush.isEraser) Color.Gray.copy(0.4f) else Color(brushColor),
                                style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    Text("F${currentIndex + 1}/${frames.size}", color = Color.White.copy(0.85f), fontSize = 11.sp)
                    Text("${activeBrush.name} · pinch zoom", color = Color.White.copy(0.5f), fontSize = 9.sp)
                    if (isRecording) Text("PERFORM", color = Color.Red, fontSize = 9.sp)
                    Text("$performRevision", color = Color.Transparent, fontSize = 1.sp)
                }
                Text(
                    if (onionEnabled) "Onion" else "Off",
                    color = if (onionEnabled) Color(0xFF03DAC6) else Color.Gray,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .clickable { flipbook.setOnionEnabled(!onionEnabled) }
                )
            }

            if (showBezier) {
                BezierEditor(
                    x1 = bezierX1, y1 = bezierY1, x2 = bezierX2, y2 = bezierY2,
                    onChange = { a, b, c, d ->
                        bezierX1 = a; bezierY1 = b; bezierX2 = c; bezierY2 = d
                        selectedEasing = EasingType.BEZIER
                    },
                    modifier = Modifier.width(160.dp).fillMaxHeight().padding(4.dp)
                )
            }
        }

        KeyframeTrackStrip(
            perform = perform,
            currentFrame = currentIndex,
            totalFrames = frames.size,
            selectedProperty = selectedProp,
            onSelectProperty = { selectedProp = it },
            onAddKeyframe = { prop, frame -> addKf(prop, frame) },
            onSelectKeyframe = { prop, kf ->
                selectedProp = prop
                selectedEasing = kf.easing
                if (kf.easing == EasingType.BEZIER) {
                    bezierX1 = kf.bx1; bezierY1 = kf.by1
                    bezierX2 = kf.bx2; bezierY2 = kf.by2
                    showBezier = true
                }
            }
        )

        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))) {
            Row(
                modifier = Modifier.fillMaxWidth().height(26.dp).background(Color(0xFF1A1A1A)).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    TimelineMode.values().forEach { mode ->
                        val sel = timelineMode == mode
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { timeline.setMode(mode) }
                            .padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text(mode.name.take(3), color = Color.White, fontSize = 8.sp)
                        }
                    }
                }
                ToolButton(if (isPlaying) "||" else ">") { timeline.togglePlay() }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ToolButton("|◀") { flipbook.previousFrame() }
                ToolButton("▶|") { flipbook.nextFrame() }
                ToolButton("+F") { flipbook.addFrame() }
                ToolButton("Dup") { flipbook.duplicateCurrentFrame() }
                ToolButton("Del") { flipbook.deleteCurrentFrame() }
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    frames.forEachIndexed { index, _ ->
                        val isCurrent = index == currentIndex
                        Box(modifier = Modifier.width(22.dp).height(18.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (isCurrent) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                            .clickable { flipbook.setCurrentFrame(index) },
                            contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = Color.White, fontSize = 8.sp)
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
            selected -> Color(0xFFE53935)
            enabled -> Color(0xFF333333)
            else -> Color(0xFF222222)
        }).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 5.dp, vertical = 3.dp)) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 9.sp)
    }
}
