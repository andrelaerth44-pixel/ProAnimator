package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.timeline.AnimProperty
import com.proanimator.core.timeline.EasingType
import com.proanimator.core.timeline.Keyframe
import com.proanimator.core.timeline.PerformEngine

/**
 * Horizontal keyframe track strip for one or more properties.
 */
@Composable
fun KeyframeTrackStrip(
    perform: PerformEngine,
    currentFrame: Int,
    totalFrames: Int,
    selectedProperty: AnimProperty,
    onSelectProperty: (AnimProperty) -> Unit,
    onAddKeyframe: (AnimProperty, Int) -> Unit,
    onSelectKeyframe: (AnimProperty, Keyframe) -> Unit,
    modifier: Modifier = Modifier
) {
    val props = AnimProperty.entries
    val frameCount = totalFrames.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF161616))
            .padding(vertical = 4.dp)
    ) {
        // Property tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            props.forEach { prop ->
                val sel = prop == selectedProperty
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (sel) Color(0xFF7C4DFF) else Color(0xFF2A2A2A))
                        .clickable { onSelectProperty(prop) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        prop.name.replace("POS_", "").take(4),
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2A2A2A))
                    .clickable { onAddKeyframe(selectedProperty, currentFrame) }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("+KF", color = Color(0xFF03DAC6), fontSize = 9.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Track canvas
        val track = perform.getTrack(selectedProperty)
        val keyframes = track.keyframes.sortedBy { it.frame }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF222222))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Playhead
                val px = ((currentFrame + 0.5f) / frameCount) * w
                drawLine(Color(0xFFBB86FC), Offset(px, 0f), Offset(px, h), 2f)

                // Keyframe diamonds
                keyframes.forEach { kf ->
                    val x = ((kf.frame + 0.5f) / frameCount) * w
                    val cy = h / 2f
                    val r = 5f
                    val color = when (kf.easing) {
                        EasingType.BEZIER -> Color(0xFFFF9800)
                        EasingType.HOLD -> Color(0xFFFF5252)
                        EasingType.LINEAR -> Color(0xFF90CAF9)
                        else -> Color(0xFF69F0AE)
                    }
                    // Diamond
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(x, cy - r)
                        lineTo(x + r, cy)
                        lineTo(x, cy + r)
                        lineTo(x - r, cy)
                        close()
                    }
                    drawPath(path, color)
                }
            }

            // Clickable keyframe overlays (approximate)
            Row(modifier = Modifier.fillMaxSize()) {
                keyframes.forEach { kf ->
                    val weight = 1f / frameCount
                    // Simple: show labels below via separate row
                }
            }
        }

        // Keyframe list for selected property
        if (keyframes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                keyframes.forEach { kf ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (kf.frame == currentFrame) Color(0xFF5C4A8A)
                                else Color(0xFF2A2A2A)
                            )
                            .clickable { onSelectKeyframe(selectedProperty, kf) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "F${kf.frame} ${kf.easing.name.take(3)} ${"%.1f".format(kf.value)}",
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}
