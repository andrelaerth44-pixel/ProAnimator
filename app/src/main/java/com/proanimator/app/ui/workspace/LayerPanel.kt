package com.proanimator.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.timeline.DrawLayer
import com.proanimator.core.timeline.LayerBlendMode

@Composable
fun LayerPanel(
    layers: List<DrawLayer>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onToggleVisible: (Int) -> Unit,
    onOpacity: (Int, Float) -> Unit,
    onBlendMode: (Int, LayerBlendMode) -> Unit = { _, _ -> },
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A1A))
            .padding(6.dp)
    ) {
        Text("Layers", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallBtn("+") { onAdd() }
            SmallBtn("−") { onRemove() }
        }
        Spacer(Modifier.height(6.dp))

        val active = layers.getOrNull(activeIndex)
        if (active != null) {
            Text(
                "Opacity ${(active.opacity * 100).toInt()}%",
                color = Color(0xFFBB86FC),
                fontSize = 9.sp
            )
            Slider(
                value = active.opacity,
                onValueChange = { onOpacity(activeIndex, it) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth().height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFBB86FC),
                    activeTrackColor = Color(0xFF7C4DFF),
                    inactiveTrackColor = Color(0xFF333333)
                )
            )
            // Blend mode cycle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2A2A2A))
                    .clickable {
                        val modes = LayerBlendMode.entries
                        val next = modes[(active.blendMode.ordinal + 1) % modes.size]
                        onBlendMode(activeIndex, next)
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    "Blend: ${active.blendMode.name.take(8)}",
                    color = Color(0xFF03DAC6),
                    fontSize = 9.sp
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            layers.asReversed().forEachIndexed { rev, _ ->
                val index = layers.lastIndex - rev
                val layer = layers[index]
                val sel = index == activeIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) Color(0xFF4A148C) else Color(0xFF2A2A2A))
                        .border(
                            1.dp,
                            if (sel) Color(0xFFBB86FC) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelect(index) }
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            layer.name.take(8),
                            color = if (layer.visible) Color.White else Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            if (layer.visible) "👁" else "—",
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { onToggleVisible(index) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (layer.opacity < 0.99f) {
                            Text("${(layer.opacity * 100).toInt()}%", color = Color.Gray, fontSize = 8.sp)
                        }
                        if (layer.blendMode != LayerBlendMode.NORMAL) {
                            Text(layer.blendMode.name.take(3), color = Color(0xFF03DAC6), fontSize = 8.sp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun SmallBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}
