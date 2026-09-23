package com.proanimator.core.brushes

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

/**
 * Variable-width pressure stroke + Alpha Lock.
 * Draws short segments so width/opacity follow pressure along the path.
 */
class BrushEngine {

    private val _activeBrush = MutableStateFlow(BrushLibrary.PEN)
    val activeBrush: StateFlow<BrushPreset> = _activeBrush.asStateFlow()

    private val _color = MutableStateFlow(0xFFFFFFFF)
    val color: StateFlow<Long> = _color.asStateFlow()

    private val _sizeMul = MutableStateFlow(1f)
    val sizeMul: StateFlow<Float> = _sizeMul.asStateFlow()

    private val _alphaLock = MutableStateFlow(false)
    val alphaLock: StateFlow<Boolean> = _alphaLock.asStateFlow()

    fun setBrush(preset: BrushPreset) { _activeBrush.value = preset }
    fun setColor(c: Long) { _color.value = c }
    fun setSizeMultiplier(m: Float) { _sizeMul.value = m.coerceIn(0.25f, 4f) }
    fun setAlphaLock(enabled: Boolean) { _alphaLock.value = enabled }
    fun toggleAlphaLock() { _alphaLock.value = !_alphaLock.value }

    fun drawStroke(bitmap: ImageBitmap, rawPoints: List<StrokePoint>) {
        if (rawPoints.size < 2) return
        val brush = _activeBrush.value
        val points = StrokeSmoother.process(rawPoints, brush)
        if (points.size < 2) return

        val canvas = Canvas(bitmap)
        canvas.nativeCanvas.saveLayer(null, null)

        // Segmented stroke → width follows pressure
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val dist = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
            if (dist < 0.3f) continue

            val pressure = ((a.pressure + b.pressure) * 0.5f).coerceIn(0.1f, 1f)
            val width = brush.sizeForPressure(pressure) * _sizeMul.value
            val alpha = brush.opacityForPressure(pressure)

            val path = Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
            }

            val paint = Paint().apply {
                strokeWidth = width
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                style = PaintingStyle.Stroke
                isAntiAlias = true
            }

            if (brush.isEraser) {
                paint.blendMode = BlendMode.Clear
                paint.color = Color.Transparent
            } else if (_alphaLock.value) {
                paint.blendMode = BlendMode.SrcIn
                paint.color = Color(_color.value).copy(alpha = alpha)
            } else {
                paint.blendMode = BlendMode.SrcOver
                paint.color = Color(_color.value).copy(alpha = alpha)
            }

            canvas.drawPath(path, paint)
        }

        canvas.nativeCanvas.restore()
    }
}
