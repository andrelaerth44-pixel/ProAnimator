package com.proanimator.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun LayerPanel(
    layers: List<DrawLayer>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onToggleVisible: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
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
        layers.asReversed().forEachIndexed { rev, _ ->
            val index = layers.lastIndex - rev
            val layer = layers[index]
            val sel = index == activeIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Color(0xFF4A148C) else Color(0xFF2A2A2A))
                    .border(1.dp, if (sel) Color(0xFFBB86FC) else Color.Transparent, RoundedCornerShape(4.dp))
                    .clickable { onSelect(index) }
                    .padding(6.dp),
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
            Spacer(Modifier.height(3.dp))
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
