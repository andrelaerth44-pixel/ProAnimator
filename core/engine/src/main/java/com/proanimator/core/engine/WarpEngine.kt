package com.proanimator.core.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Real mesh warp / liquify via Android/Skia [Canvas.drawBitmapMesh].
 *
 * Grid of verts is deformed with a Gaussian falloff brush (push mode).
 * drawBitmapMesh is hardware-accelerated through Skia — not a CPU pixel loop.
 */
class WarpEngine(
    val cols: Int = 24,
    val rows: Int = 24
) {
    /** Vertex positions: length = (cols+1)*(rows+1)*2  (x,y pairs) in bitmap space */
    private var verts: FloatArray = FloatArray(0)
    private var width: Int = 0
    private var height: Int = 0
    private var initialized = false

    val meshCols: Int get() = cols
    val meshRows: Int get() = rows

    fun ensureMesh(bmpW: Int, bmpH: Int) {
        if (initialized && width == bmpW && height == bmpH) return
        width = bmpW
        height = bmpH
        val vx = cols + 1
        val vy = rows + 1
        verts = FloatArray(vx * vy * 2)
        var i = 0
        for (y in 0 until vy) {
            for (x in 0 until vx) {
                verts[i++] = x * (bmpW - 1f) / cols
                verts[i++] = y * (bmpH - 1f) / rows
            }
        }
        initialized = true
    }

    fun resetMesh() {
        if (!initialized) return
        val vx = cols + 1
        val vy = rows + 1
        var i = 0
        for (y in 0 until vy) {
            for (x in 0 until vx) {
                verts[i++] = x * (width - 1f) / cols
                verts[i++] = y * (height - 1f) / rows
            }
        }
    }

    /**
     * Liquify push: displace verts near (cx,cy) by (dx,dy) with Gaussian falloff.
     * @param radius brush radius in bitmap pixels
     * @param strength 0..1 how much of (dx,dy) to apply at center
     */
    fun push(
        cx: Float,
        cy: Float,
        dx: Float,
        dy: Float,
        radius: Float = 80f,
        strength: Float = 0.85f
    ) {
        if (!initialized) return
        val r2 = radius * radius
        val invR2 = 1f / max(r2, 1f)
        val vx = cols + 1
        val vy = rows + 1
        for (y in 0 until vy) {
            for (x in 0 until vx) {
                val idx = (y * vx + x) * 2
                val px = verts[idx]
                val py = verts[idx + 1]
                val dist2 = (px - cx) * (px - cx) + (py - cy) * (py - cy)
                if (dist2 > r2) continue
                val w = exp(-dist2 * invR2 * 2.5f).toFloat() * strength
                verts[idx] = px + dx * w
                verts[idx + 1] = py + dy * w
            }
        }
    }

    /**
     * Pinch / bloat around point.
     * @param amount >0 bloat, <0 pinch
     */
    fun pinchBloat(
        cx: Float,
        cy: Float,
        amount: Float,
        radius: Float = 100f
    ) {
        if (!initialized) return
        val r = max(radius, 1f)
        val vx = cols + 1
        val vy = rows + 1
        for (y in 0 until vy) {
            for (x in 0 until vx) {
                val idx = (y * vx + x) * 2
                val px = verts[idx]
                val py = verts[idx + 1]
                val ddx = px - cx
                val ddy = py - cy
                val dist = hypot(ddx.toDouble(), ddy.toDouble()).toFloat()
                if (dist > r || dist < 0.001f) continue
                val t = 1f - dist / r
                val fall = t * t
                val scale = 1f + amount * fall
                verts[idx] = cx + ddx * scale
                verts[idx + 1] = cy + ddy * scale
            }
        }
    }

    /**
     * Rasterize warped bitmap using Skia drawBitmapMesh.
     */
    fun apply(source: ImageBitmap): ImageBitmap {
        val src = source.asAndroidBitmap()
        ensureMesh(src.width, src.height)
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        // drawBitmapMesh(bitmap, meshWidth, meshHeight, verts, vertOffset, colors, colorOffset, paint)
        // meshWidth = cols, meshHeight = rows → (cols+1)*(rows+1) vertices
        canvas.drawBitmapMesh(
            src,
            cols,
            rows,
            verts,
            0,
            null,
            0,
            paint
        )
        return out.asImageBitmap()
    }

    /** Copy current verts (for undo of warp session) */
    fun snapshotVerts(): FloatArray = verts.copyOf()

    fun restoreVerts(snapshot: FloatArray) {
        if (snapshot.size == verts.size) {
            System.arraycopy(snapshot, 0, verts, 0, verts.size)
        }
    }

    fun isReady(): Boolean = initialized
}
