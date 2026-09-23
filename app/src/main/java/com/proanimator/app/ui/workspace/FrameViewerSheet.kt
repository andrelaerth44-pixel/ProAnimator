package com.proanimator.app.ui.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.FrameClipboard

/**
 * Full-screen Frame Viewer (FlipaClip-style modal).
 * Multi-select, copy, paste append, delete, duplicate — does not clutter the main stage.
 */
@Composable
fun FrameViewerSheet(
    flipbook: FlipbookBitmapEngine,
    clipboard: FrameClipboard,
    onClose: () -> Unit,
    onStatus: (String) -> Unit
) {
    val frames = flipbook.frames.value
    val current = flipbook.currentIndex.value
    var selected by remember { mutableStateOf(setOf(current)) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Frames (${frames.size})",
                color = Color(0xFFBB86FC),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Close",
                color = Color(0xFF03DAC6),
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Chip("All") { selected = frames.indices.toSet() }
            Chip("None") { selected = emptySet() }
            Chip("Copy") {
                if (selected.isEmpty()) onStatus("Nothing selected")
                else {
                    clipboard.copyFrom(flipbook, selected)
                    onStatus("Copied ${selected.size} frame(s)")
                }
            }
            Chip("Paste+") {
                if (clipboard.isEmpty) onStatus("Clipboard empty")
                else {
                    clipboard.pasteAppend(flipbook)
                    onStatus("Pasted ${clipboard.size} at end")
                }
            }
            Chip("Dup") {
                val idx = selected.minOrNull() ?: return@Chip
                flipbook.setCurrentFrame(idx)
                flipbook.duplicateCurrentFrame()
                onStatus("Duplicated F$idx")
            }
            Chip("Del") {
                val ordered = selected.sortedDescending()
                if (ordered.isEmpty() || frames.size <= 1) {
                    onStatus("Cannot delete")
                    return@Chip
                }
                ordered.forEach { i ->
                    if (flipbook.frames.value.size > 1) {
                        flipbook.setCurrentFrame(i.coerceAtMost(flipbook.frames.value.lastIndex))
                        flipbook.deleteCurrentFrame()
                    }
                }
                selected = emptySet()
                onStatus("Deleted")
            }
            Chip("+F") {
                flipbook.addFrame()
                onStatus("Frame added")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(frames, key = { _, f -> f.id }) { index, frame ->
                val isSel = index in selected
                val isCur = index == flipbook.currentIndex.value
                Box(
                    Modifier
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = when {
                                isSel -> Color(0xFFBB86FC)
                                isCur -> Color(0xFF03DAC6)
                                else -> Color(0xFF444444)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            selected = if (isSel) selected - index else selected + index
                            flipbook.setCurrentFrame(index)
                        }
                ) {
                    Image(
                        bitmap = frame.bitmap,
                        contentDescription = "F$index",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A))
                    )
                    Text(
                        "F$index",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color(0xAA000000))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF333333))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}
