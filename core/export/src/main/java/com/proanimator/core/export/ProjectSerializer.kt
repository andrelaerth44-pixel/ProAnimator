package com.proanimator.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * ProjectSerializer — Phase 3 Save/Load
 *
 * Format: .pan (ProAnimator Project)
 * Structure (binary + gzip):
 *   MAGIC "PAN1" (4 bytes)
 *   version (int)
 *   width (int)
 *   height (int)
 *   fps (float)
 *   currentFrameIndex (int)
 *   frameCount (int)
 *   for each frame:
 *     pngLength (int)
 *     pngBytes (pngLength bytes)  — lossless PNG of the frame bitmap
 *   metaJsonLength (int)
 *   metaJson (UTF-8) — extra timeline / keyframe metadata
 *
 * This keeps bitmaps lossless and metadata flexible.
 */
class ProjectSerializer(private val context: Context) {

    companion object {
        private const val MAGIC = "PAN1"
        private const val VERSION = 1
        private const val EXT = ".pan"
    }

    data class ProjectData(
        val width: Int,
        val height: Int,
        val fps: Float,
        val currentFrameIndex: Int,
        val frames: List<ImageBitmap>,
        val meta: JSONObject = JSONObject()
    )

    suspend fun save(
        data: ProjectData,
        fileName: String = "project_${System.currentTimeMillis()}"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "projects")
            dir.mkdirs()
            val file = File(dir, if (fileName.endsWith(EXT)) fileName else "$fileName$EXT")

            FileOutputStream(file).use { fos ->
                GZIPOutputStream(fos).use { gzos ->
                    DataOutputStream(gzos).use { out ->
                        // Header
                        out.writeBytes(MAGIC)
                        out.writeInt(VERSION)
                        out.writeInt(data.width)
                        out.writeInt(data.height)
                        out.writeFloat(data.fps)
                        out.writeInt(data.currentFrameIndex.coerceIn(0, data.frames.size - 1))
                        out.writeInt(data.frames.size)

                        // Frames as PNG
                        data.frames.forEach { imageBitmap ->
                            val bmp = imageBitmap.asAndroidBitmap()
                            val baos = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                            val png = baos.toByteArray()
                            out.writeInt(png.size)
                            out.write(png)
                        }

                        // Metadata JSON
                        val metaBytes = data.meta.toString().toByteArray(Charsets.UTF_8)
                        out.writeInt(metaBytes.size)
                        out.write(metaBytes)
                    }
                }
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun load(file: File): Result<ProjectData> = withContext(Dispatchers.IO) {
        try {
            FileInputStream(file).use { fis ->
                GZIPInputStream(fis).use { gzis ->
                    DataInputStream(gzis).use { input ->
                        // Magic
                        val magicBytes = ByteArray(4)
                        input.readFully(magicBytes)
                        val magic = String(magicBytes, Charsets.US_ASCII)
                        if (magic != MAGIC) {
                            return@withContext Result.failure(
                                IllegalArgumentException("Not a valid .pan file (magic=$magic)")
                            )
                        }

                        val version = input.readInt()
                        if (version > VERSION) {
                            return@withContext Result.failure(
                                IllegalArgumentException("Unsupported .pan version $version")
                            )
                        }

                        val width = input.readInt()
                        val height = input.readInt()
                        val fps = input.readFloat()
                        val currentFrameIndex = input.readInt()
                        val frameCount = input.readInt()

                        val frames = mutableListOf<ImageBitmap>()
                        repeat(frameCount) {
                            val pngLen = input.readInt()
                            val pngBytes = ByteArray(pngLen)
                            input.readFully(pngBytes)
                            val bmp = BitmapFactory.decodeByteArray(pngBytes, 0, pngLen)
                                ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            frames.add(bmp.asImageBitmap())
                        }

                        val metaLen = input.readInt()
                        val meta = if (metaLen > 0) {
                            val metaBytes = ByteArray(metaLen)
                            input.readFully(metaBytes)
                            JSONObject(String(metaBytes, Charsets.UTF_8))
                        } else JSONObject()

                        Result.success(
                            ProjectData(
                                width = width,
                                height = height,
                                fps = fps,
                                currentFrameIndex = currentFrameIndex.coerceIn(0, frames.size - 1),
                                frames = frames,
                                meta = meta
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listProjects(): List<File> {
        val dir = File(context.filesDir, "projects")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.equals("pan", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteProject(file: File): Boolean {
        return try { file.delete() } catch (_: Exception) { false }
    }
}
