package com.proanimator.core.timeline

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.UUID

enum class LayerBlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    ADD,
    DARKEN,
    LIGHTEN
}

data class DrawLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bitmap: ImageBitmap,
    var visible: Boolean = true,
    var opacity: Float = 1f,
    var blendMode: LayerBlendMode = LayerBlendMode.NORMAL
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

    fun setLayerBlendMode(index: Int, mode: LayerBlendMode) {
        if (index in layers.indices) {
            val L = layers[index]
            layers[index] = L.copy(blendMode = mode)
        }
    }

    fun setLayerMeta(
        index: Int,
        name: String,
        visible: Boolean,
        opacity: Float,
        blendMode: LayerBlendMode = LayerBlendMode.NORMAL
    ) {
        if (index !in layers.indices) return
        val L = layers[index]
        layers[index] = L.copy(
            name = name,
            visible = visible,
            opacity = opacity.coerceIn(0f, 1f),
            blendMode = blendMode
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
                xfermode = porterDuff(layer.blendMode)
            }
            c.drawBitmap(layer.bitmap.asAndroidBitmap(), 0f, 0f, paint)
        }
        return out.asImageBitmap()
    }

    private fun porterDuff(mode: LayerBlendMode): PorterDuffXfermode? {
        val pd = when (mode) {
            LayerBlendMode.NORMAL -> return null
            LayerBlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            LayerBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            LayerBlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            LayerBlendMode.ADD -> PorterDuff.Mode.ADD
            LayerBlendMode.DARKEN -> PorterDuff.Mode.DARKEN
            LayerBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
        }
        return PorterDuffXfermode(pd)
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

    fun replaceAll(from: LayerStack) {
        duplicateFrom(from)
    }

    private fun emptyBitmap(): ImageBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
