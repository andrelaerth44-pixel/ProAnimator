package com.proanimator.core.lottie

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.max

/**
 * Import Lottie JSON → sequence of ImageBitmaps for Flipbook.
 *
 * Research:
 * - airbnb/lottie-android LottieComposition + LottieDrawable
 * - Rasterize each frame to ARGB bitmap (AnimaX-inspired "bring AE assets in")
 * - Does NOT depend on AnimaX C++ engine; uses stable Lottie Android player
 */
class LottieFrameImporter(private val context: Context) {

    data class ImportResult(
        val frames: List<ImageBitmap>,
        val fps: Float,
        val width: Int,
        val height: Int,
        val name: String
    )

    suspend fun importFromAssets(
        assetName: String,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        maxFrames: Int = 120
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val task = LottieCompositionFactory.fromAsset(context, assetName)
            val composition = task.result
                ?: return@withContext Result.failure(IllegalStateException("Failed to parse $assetName"))
            rasterize(composition, assetName, targetWidth, targetHeight, maxFrames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromFile(
        file: File,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        maxFrames: Int = 120
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            file.inputStream().use { stream ->
                importFromStream(stream, file.name, targetWidth, targetHeight, maxFrames)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromStream(
        stream: InputStream,
        name: String = "lottie",
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        maxFrames: Int = 120
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val task = LottieCompositionFactory.fromJsonInputStream(stream, name)
            val composition = task.result
                ?: return@withContext Result.failure(IllegalStateException("Failed to parse Lottie"))
            rasterize(composition, name, targetWidth, targetHeight, maxFrames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun rasterize(
        composition: LottieComposition,
        name: String,
        targetWidth: Int?,
        targetHeight: Int?,
        maxFrames: Int
    ): Result<ImportResult> {
        val bounds = composition.bounds
        val srcW = max(bounds.width(), 1)
        val srcH = max(bounds.height(), 1)
        val w = targetWidth ?: srcW
        val h = targetHeight ?: srcH

        val durationFrames = composition.durationFrames
        val fps = composition.frameRate
        val total = durationFrames.toInt().coerceAtLeast(1)
        val step = if (total > maxFrames) total.toFloat() / maxFrames else 1f
        val count = minOf(total, maxFrames)

        val drawable = LottieDrawable().apply {
            setComposition(composition)
            // Fit into target
            setBounds(0, 0, w, h)
        }

        val frames = mutableListOf<ImageBitmap>()
        for (i in 0 until count) {
            val frame = (i * step).coerceIn(0f, durationFrames - 0.001f)
            drawable.frame = frame.toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.TRANSPARENT)
            // Scale drawable to target size
            val scaleX = w.toFloat() / srcW
            val scaleY = h.toFloat() / srcH
            canvas.save()
            canvas.scale(scaleX, scaleY)
            drawable.setBounds(0, 0, srcW, srcH)
            drawable.draw(canvas)
            canvas.restore()
            frames.add(bmp.asImageBitmap())
        }

        return Result.success(
            ImportResult(
                frames = frames,
                fps = fps,
                width = w,
                height = h,
                name = name
            )
        )
    }
}
