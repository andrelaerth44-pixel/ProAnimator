package com.proanimator.core.brushes

import com.proanimator.domain.model.StrokePoint
import kotlin.math.sqrt

/**
 * Stroke smoothing inspired by freehand drawing engines:
 * - Distance filter (drop near-duplicate points)
 * - Simple exponential moving average
 * - Optional Catmull-Rom style midpoints for denser curves
 */
object StrokeSmoother {

    fun filterByDistance(points: List<StrokePoint>, minDist: Float = 1.5f): List<StrokePoint> {
        if (points.size < 2) return points
        val out = mutableListOf(points.first())
        for (i in 1 until points.size) {
            val prev = out.last()
            val cur = points[i]
            val dx = cur.x - prev.x
            val dy = cur.y - prev.y
            if (dx * dx + dy * dy >= minDist * minDist) {
                out.add(cur)
            }
        }
        if (out.last() !== points.last()) out.add(points.last())
        return out
    }

    fun smoothEma(points: List<StrokePoint>, factor: Float): List<StrokePoint> {
        if (points.size < 3 || factor <= 0f) return points
        val a = factor.coerceIn(0.05f, 0.95f)
        val out = mutableListOf(points.first())
        var sx = points.first().x
        var sy = points.first().y
        for (i in 1 until points.size) {
            val p = points[i]
            sx = a * p.x + (1 - a) * sx
            sy = a * p.y + (1 - a) * sy
            out.add(StrokePoint(sx, sy, p.pressure))
        }
        return out
    }

    fun process(points: List<StrokePoint>, brush: BrushPreset): List<StrokePoint> {
        val filtered = filterByDistance(points, 1.2f)
        return if (brush.smoothing > 0.01f) {
            smoothEma(filtered, brush.smoothing)
        } else filtered
    }

    fun segmentLength(a: StrokePoint, b: StrokePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }
}
