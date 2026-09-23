package com.proanimator.core.timeline

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.PorterDuff
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.UUID

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
        repeat(initialLayers.coerceAtLeast(0)) { i ->
            layers.add(DrawLayer(name = "Layer ${i + 1}", bitmap = emptyBitmap()))
        }
        if (layers.isNotEmpty()) activeLayerIndex = 0
    }

    fun layerCount(): Int = layers.size

    fun layers(): List<DrawLayer> = layers.toList()

    fun activeLayer(): DrawLayer {
        if (layers.isEmpty()) {
            layers.add(DrawLayer(name = "Layer 1", bitmap = emptyBitmap()))
            activeLayerIndex = 0
        }
        return layers[activeLayerIndex.coerceIn(0, layers.lastIndex)]
    }

    fun setActive(index: Int) {
        if (layers.isEmpty()) return
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
            val L = layers[index]
            layers[index] = L.copy(visible = !L.visible)
        }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        if (index in layers.indices) {
            val L = layers[index]
            layers[index] = L.copy(opacity = opacity.coerceIn(0f, 1f))
        }
    }

    fun setLayerMeta(index: Int, name: String, visible: Boolean, opacity: Float) {
        if (index !in layers.indices) return
        val L = layers[index]
        layers[index] = L.copy(
            name = name,
            visible = visible,
            opacity = opacity.coerceIn(0f, 1f)
        )
    }

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
        if (layers.isEmpty()) {
            layers.add(DrawLayer(name = "Layer 1", bitmap = bmp))
            activeLayerIndex = 0
            return
        }
        val i = activeLayerIndex.coerceIn(0, layers.lastIndex)
        layers[i] = layers[i].copy(bitmap = bmp)
    }

    fun snapshotActive(): Bitmap =
        activeLayer().bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)

    fun restoreActive(bmp: Bitmap) {
        val i = activeLayerIndex.coerceIn(0, layers.lastIndex.coerceAtLeast(0))
        if (layers.isEmpty()) {
            layers.add(DrawLayer(name = "Layer 1", bitmap = bmp.asImageBitmap()))
            return
        }
        layers[i] = layers[i].copy(bitmap = bmp.asImageBitmap())
    }

    fun duplicateFrom(other: LayerStack) {
        layers.clear()
        other.layers.forEach { src ->
            val copy = src.bitmap.asAndroidBitmap()
                .copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
            layers.add(src.copy(id = UUID.randomUUID().toString(), bitmap = copy))
        }
        if (layers.isEmpty()) {
            layers.add(DrawLayer(name = "Layer 1", bitmap = emptyBitmap()))
        }
        activeLayerIndex = other.activeLayerIndex.coerceIn(0, layers.lastIndex)
    }

    /** Restore full stack from PAN2 load */
    fun replaceAll(from: LayerStack) {
        duplicateFrom(from)
    }

    private fun emptyBitmap(): ImageBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
