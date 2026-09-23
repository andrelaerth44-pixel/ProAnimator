package com.proanimator.core.ink

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

/**
 * Rasterizes finished Ink-like strokes onto a layer ImageBitmap.
 *
 * When Jetpack Ink is on classpath, host InProgressStrokes and call
 * [rasterize] from onStrokesFinished with extracted points/pressures.
 * Without Ink deps, this is still used as a high-quality stroke raster path.
 */
object InkRasterizer {

    data class Point(val x: Float, val y: Float, val pressure: Float = 1f)

    fun rasterize(
        target: ImageBitmap,
        points: List<Point>,
        color: Long = 0xFFFFFFFF,
        baseWidth: Float = 8f,
        isEraser: Boolean = false
    ) {
        if (points.size < 2) return
        val canvas = Canvas(target)
        canvas.nativeCanvas.saveLayer(null, null)

        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val pressure = ((a.pressure + b.pressure) * 0.5f).coerceIn(0.1f, 1f)
            val width = baseWidth * (0.35f + 0.65f * pressure)

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
                if (isEraser) {
                    blendMode = BlendMode.Clear
                    this.color = Color.Transparent
                } else {
                    blendMode = BlendMode.SrcOver
                    this.color = Color(color).copy(alpha = 0.4f + 0.6f * pressure)
                }
            }
            canvas.drawPath(path, paint)
        }
        canvas.nativeCanvas.restore()
    }
}
