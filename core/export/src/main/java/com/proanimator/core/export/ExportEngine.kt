package com.proanimator.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat {
    PNG_SEQUENCE,
    // Future: MP4, GIF, WEBM
}

data class ExportSettings(
    val format: ExportFormat = ExportFormat.PNG_SEQUENCE,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Float = 24f,
    val transparentBackground: Boolean = true,
    val startFrame: Int = 0,
    val endFrame: Int = 119
)

data class ExportProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val message: String = ""
)

/**
 * ExportEngine - Phase 3 foundation.
 * Currently supports PNG sequence export.
 * Architecture ready for MediaCodec (MP4) later.
 */
class ExportEngine(private val context: Context) {

    /**
     * Renders a list of strokes into a Bitmap.
     * This is the core rendering function that will later use ImageBitmap / hardware canvas.
     */
    fun renderFrame(
        strokes: List<Stroke>,
        width: Int,
        height: Int,
        transparent: Boolean = true,
        backgroundColor: Int = Color.BLACK
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (transparent) {
            canvas.drawColor(Color.TRANSPARENT)
        } else {
            canvas.drawColor(backgroundColor)
        }

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach

            paint.color = stroke.color.toInt()
            paint.alpha = (stroke.opacity * 255).toInt().coerceIn(0, 255)
            paint.strokeWidth = stroke.size

            val path = Path()
            path.moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x, stroke.points[i].y)
            }
            canvas.drawPath(path, paint)
        }

        return bitmap
    }

    /**
     * Exports a sequence of frames as PNG files.
     * @param framesProvider function that returns the list of strokes for a given frame index
     */
    suspend fun exportPngSequence(
        settings: ExportSettings,
        framesProvider: (frameIndex: Int) -> List<Stroke>,
        onProgress: (ExportProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.getExternalFilesDir(null), "exports/png_sequence_${System.currentTimeMillis()}")
            if (!exportDir.exists()) exportDir.mkdirs()

            val total = (settings.endFrame - settings.startFrame + 1).coerceAtLeast(1)

            for (i in settings.startFrame..settings.endFrame) {
                val relativeIndex = i - settings.startFrame
                onProgress(ExportProgress(relativeIndex + 1, total, "Rendering frame ${i + 1}"))

                val strokes = framesProvider(i)
                val bitmap = renderFrame(
                    strokes = strokes,
                    width = settings.width,
                    height = settings.height,
                    transparent = settings.transparentBackground
                )

                val fileName = "frame_%04d.png".format(relativeIndex)
                val file = File(exportDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
            }

            onProgress(ExportProgress(total, total, "Export complete"))
            Result.success(exportDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quick single-frame export (current frame as PNG).
     */
    suspend fun exportCurrentFrameAsPng(
        strokes: List<Stroke>,
        width: Int = 1920,
        height: Int = 1080,
        transparent: Boolean = true
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderFrame(strokes, width, height, transparent)
            val file = File(context.getExternalFilesDir(null), "exports/frame_${System.currentTimeMillis()}.png")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
