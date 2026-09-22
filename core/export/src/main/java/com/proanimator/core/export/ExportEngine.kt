package com.proanimator.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.proanimator.domain.model.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat {
    PNG_SEQUENCE,
    MP4
}

data class ExportSettings(
    val format: ExportFormat = ExportFormat.PNG_SEQUENCE,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Float = 24f,
    val transparentBackground: Boolean = true
)

data class ExportProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val message: String = ""
)

/**
 * ExportEngine — Phase 3
 * Supports PNG sequence and MP4 (via Mp4Encoder).
 */
class ExportEngine(private val context: Context) {

    private val mp4Encoder = Mp4Encoder(context)

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

    suspend fun exportPngSequence(
        bitmaps: List<ImageBitmap>,
        onProgress: (ExportProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(
                context.getExternalFilesDir(null),
                "exports/png_sequence_${System.currentTimeMillis()}"
            )
            exportDir.mkdirs()

            bitmaps.forEachIndexed { index, imageBitmap ->
                onProgress(ExportProgress(index + 1, bitmaps.size, "PNG frame ${index + 1}"))
                val androidBmp = imageBitmap.asAndroidBitmap()
                val file = File(exportDir, "frame_%04d.png".format(index))
                FileOutputStream(file).use { out ->
                    androidBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            onProgress(ExportProgress(bitmaps.size, bitmaps.size, "PNG sequence done"))
            Result.success(exportDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportMp4(
        bitmaps: List<ImageBitmap>,
        width: Int = 1920,
        height: Int = 1080,
        fps: Float = 24f,
        onProgress: (ExportProgress) -> Unit = {}
    ): Result<File> {
        return mp4Encoder.encode(
            frames = bitmaps,
            width = width,
            height = height,
            fps = fps,
            onProgress = { p ->
                onProgress(ExportProgress(p.current, p.total, p.message))
            }
        )
    }

    suspend fun exportCurrentFrameAsPng(
        bitmap: ImageBitmap
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(
                context.getExternalFilesDir(null),
                "exports/frame_${System.currentTimeMillis()}.png"
            )
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
