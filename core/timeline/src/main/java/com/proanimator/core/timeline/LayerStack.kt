package com.proanimator.core.timeline

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.PorterDuff
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.UUID

/**
 * Multi-layer stack per Flipbook frame.
 * Drawing targets [activeLayerIndex]; composite for display/export/onion.
 */
data class DrawLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bitmap: ImageBitmap,
    var visible: Boolean = true,
    var opacity: Float = 1f
)

class LayerStack(
    val width: Int,
    val height: Int,
    initialLayers: Int = 1
) {
    private val layers = mutableListOf<DrawLayer>()
    var activeLayerIndex: Int = 0
        private set

    init {
        repeat(initialLayers.coerceAtLeast(1)) { i ->
            layers.add(DrawLayer(name = "Layer ${i + 1}", bitmap = emptyBitmap()))
        }
    }

    fun layerCount(): Int = layers.size

    fun layers(): List<DrawLayer> = layers.toList()

    fun activeLayer(): DrawLayer = layers[activeLayerIndex.coerceIn(0, layers.lastIndex)]

    fun setActive(index: Int) {
        activeLayerIndex = index.coerceIn(0, layers.lastIndex)
    }

    fun addLayer(name: String? = null) {
        layers.add(DrawLayer(name = name ?: "Layer ${layers.size + 1}", bitmap = emptyBitmap()))
        activeLayerIndex = layers.lastIndex
    }

    fun removeLayer(index: Int) {
        if (layers.size <= 1) return
        if (index !in layers.indices) return
        layers.removeAt(index)
        if (activeLayerIndex >= layers.size) activeLayerIndex = layers.lastIndex
    }

    fun toggleVisibility(index: Int) {
        if (index in layers.indices) {
            layers[index] = layers[index].copy(visible = !layers[index].visible)
        }
    }

    /** Flatten visible layers bottom→top for onion/export */
    fun composite(): ImageBitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = AndroidCanvas(out)
        c.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
        layers.forEach { layer ->
            if (!layer.visible) return@forEach
            val paint = android.graphics.Paint().apply {
                alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            }
            c.drawBitmap(layer.bitmap.asAndroidBitmap(), 0f, 0f, paint)
        }
        return out.asImageBitmap()
    }

    fun replaceActiveBitmap(bmp: ImageBitmap) {
        val i = activeLayerIndex
        layers[i] = layers[i].copy(bitmap = bmp)
    }

    fun snapshotActive(): Bitmap =
        activeLayer().bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)

    fun restoreActive(bmp: Bitmap) {
        layers[activeLayerIndex] = activeLayer().copy(bitmap = bmp.asImageBitmap())
    }

    fun duplicateFrom(other: LayerStack) {
        layers.clear()
        other.layers.forEach { src ->
            val copy = src.bitmap.asAndroidBitmap()
                .copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
            layers.add(src.copy(id = UUID.randomUUID().toString(), bitmap = copy))
        }
        activeLayerIndex = other.activeLayerIndex.coerceIn(0, layers.lastIndex)
    }

    private fun emptyBitmap(): ImageBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
