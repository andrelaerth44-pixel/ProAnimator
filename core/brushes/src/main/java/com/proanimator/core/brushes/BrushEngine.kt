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

/**
 * Applies BrushPreset strokes onto ImageBitmap with pressure-aware width/opacity.
 */
class BrushEngine {

    private val _activeBrush = MutableStateFlow(BrushLibrary.PEN)
    val activeBrush: StateFlow<BrushPreset> = _activeBrush.asStateFlow()

    private val _color = MutableStateFlow(0xFFFFFFFF)
    val color: StateFlow<Long> = _color.asStateFlow()

    private val _sizeMul = MutableStateFlow(1f)
    val sizeMul: StateFlow<Float> = _sizeMul.asStateFlow()

    fun setBrush(preset: BrushPreset) { _activeBrush.value = preset }
    fun setColor(c: Long) { _color.value = c }
    fun setSizeMultiplier(m: Float) { _sizeMul.value = m.coerceIn(0.25f, 4f) }

    fun drawStroke(bitmap: ImageBitmap, rawPoints: List<StrokePoint>) {
        if (rawPoints.size < 2) return
        val brush = _activeBrush.value
        val points = StrokeSmoother.process(rawPoints, brush)
        if (points.size < 2) return

        val canvas = Canvas(bitmap)
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        val avgPressure = points.map { it.pressure }.average().toFloat().coerceIn(0.15f, 1f)
        val width = brush.sizeForPressure(avgPressure) * _sizeMul.value
        val alpha = brush.opacityForPressure(avgPressure)

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
        } else {
            paint.blendMode = BlendMode.SrcOver
            paint.color = Color(_color.value).copy(alpha = alpha)
        }

        canvas.nativeCanvas.saveLayer(null, null)
        canvas.drawPath(path, paint)
        canvas.nativeCanvas.restore()
    }
}
