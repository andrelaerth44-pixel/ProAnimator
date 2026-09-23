package com.proanimator.core.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Bake TransformTool state into a layer bitmap (translate/scale/rotate around center).
 */
object TransformBake {

    fun bake(
        source: ImageBitmap,
        tx: Float,
        ty: Float,
        scale: Float,
        rotationDeg: Float
    ): ImageBitmap {
        val src = source.asAndroidBitmap()
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val matrix = Matrix().apply {
            postTranslate(-w / 2f, -h / 2f)
            postScale(scale, scale)
            postRotate(rotationDeg)
            postTranslate(w / 2f + tx, h / 2f + ty)
        }
        canvas.drawBitmap(src, matrix, paint)
        return out.asImageBitmap()
    }
}
