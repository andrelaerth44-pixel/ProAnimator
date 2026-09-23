package com.proanimator.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.engine.TransformTool

@Composable
fun TransformOverlay(
    tool: TransformTool,
    onApplyHint: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by tool.state.collectAsState()
    if (!state.enabled) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    tool.drag(pan.x, pan.y)
                    if (zoom != 1f) tool.pinch(zoom)
                    if (rotation != 0f) tool.rotate(rotation)
                }
            }
    ) {
        // HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xEE1A1A1A))
                .border(1.dp, Color(0xFF7C4DFF), RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TRANSFORM", color = Color(0xFFBB86FC), fontSize = 11.sp)
            Text(
                "T ${state.tx.toInt()},${state.ty.toInt()}  S ${"%.2f".format(state.scale)}  R ${state.rotation.toInt()}°",
                color = Color.White,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF333333))
                        .clickable { tool.reset() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("Reset", color = Color.White, fontSize = 10.sp) }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF7C4DFF))
                        .clickable {
                            onApplyHint()
                            tool.disable()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("Done", color = Color.White, fontSize = 10.sp) }
            }
            Text("Drag / pinch / rotate", color = Color.Gray, fontSize = 9.sp)
        }
    }
}
