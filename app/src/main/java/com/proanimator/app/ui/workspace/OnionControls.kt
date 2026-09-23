package com.proanimator.app.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnionControls(
    enabled: Boolean,
    before: Int,
    after: Int,
    onToggle: () -> Unit,
    onBefore: (Int) -> Unit,
    onAfter: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (enabled) "Onion ON" else "Onion OFF",
            color = if (enabled) Color(0xFF03DAC6) else Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.clickable(onClick = onToggle)
        )
        Text("←", color = Color(0xFFFF5252), fontSize = 11.sp)
        Stepper(before, 0, 5, onBefore)
        Text("→", color = Color(0xFF69F0AE), fontSize = 11.sp)
        Stepper(after, 0, 5, onAfter)
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF333333))
                .clickable { onChange((value - 1).coerceAtLeast(min)) }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) { Text("−", color = Color.White, fontSize = 11.sp) }
        Text("$value", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF333333))
                .clickable { onChange((value + 1).coerceAtMost(max)) }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) { Text("+", color = Color.White, fontSize = 11.sp) }
    }
}
