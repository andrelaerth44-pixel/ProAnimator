package com.proanimator.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.proanimator.core.timeline.AnimProperty
import com.proanimator.core.timeline.EasingType
import com.proanimator.core.timeline.FlipbookBitmapEngine
import com.proanimator.core.timeline.Keyframe
import com.proanimator.core.timeline.LayerStack
import com.proanimator.core.timeline.PerformEngine
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
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * .pan format
 *
 * PAN1: composite PNG per frame
 * PAN2: per-layer PNG stacks + meta (schema 3+)
 * Meta may include "undo" from UndoArchive (eternal undo snapshots).
 */
class ProjectSerializer(private val context: Context) {

    companion object {
        private const val MAGIC_V1 = "PAN1"
        private const val MAGIC_V2 = "PAN2"
        private const val VERSION = 2
        private const val META_SCHEMA = 4
        private const val EXT = ".pan"

        fun buildMeta(
            brushId: String,
            onionEnabled: Boolean,
            timelineMode: String,
            perform: PerformEngine,
            undoJson: JSONObject? = null
        ): JSONObject {
            val tracksJson = JSONObject()
            AnimProperty.entries.forEach { prop ->
                val arr = JSONArray()
                perform.getTrack(prop).keyframes.sortedBy { it.frame }.forEach { kf ->
                    arr.put(JSONObject().apply {
                        put("frame", kf.frame)
                        put("value", kf.value.toDouble())
                        put("easing", kf.easing.name)
                        put("bx1", kf.bx1.toDouble())
                        put("by1", kf.by1.toDouble())
                        put("bx2", kf.bx2.toDouble())
                        put("by2", kf.by2.toDouble())
                    })
                }
                tracksJson.put(prop.name, arr)
            }
            return JSONObject().apply {
                put("schema", META_SCHEMA)
                put("brushId", brushId)
                put("onionEnabled", onionEnabled)
                put("timelineMode", timelineMode)
                put("tracks", tracksJson)
                if (undoJson != null) put("undo", undoJson)
            }
        }

        fun applyMeta(meta: JSONObject, perform: PerformEngine): MetaExtras {
            perform.clearAll()
            val tracks = meta.optJSONObject("tracks")
            if (tracks != null) {
                AnimProperty.entries.forEach { prop ->
                    val arr = tracks.optJSONArray(prop.name) ?: return@forEach
                    val list = perform.getTrack(prop).keyframes
                    list.clear()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val easing = try {
                            EasingType.valueOf(o.optString("easing", "EASE_IN_OUT"))
                        } catch (_: Exception) {
                            EasingType.EASE_IN_OUT
                        }
                        list.add(
                            Keyframe(
                                frame = o.getInt("frame"),
                                value = o.getDouble("value").toFloat(),
                                easing = easing,
                                bx1 = o.optDouble("bx1", 0.42).toFloat(),
                                by1 = o.optDouble("by1", 0.0).toFloat(),
                                bx2 = o.optDouble("bx2", 0.58).toFloat(),
                                by2 = o.optDouble("by2", 1.0).toFloat()
                            )
                        )
                    }
                }
            }
            return MetaExtras(
                brushId = meta.optString("brushId", "pen"),
                onionEnabled = meta.optBoolean("onionEnabled", true),
                timelineMode = meta.optString("timelineMode", "COMPOSE"),
                undo = meta.optJSONObject("undo")
            )
        }

        private fun pngBytes(bmp: ImageBitmap): ByteArray {
            val baos = ByteArrayOutputStream()
            bmp.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, baos)
            return baos.toByteArray()
        }
    }

    data class MetaExtras(
        val brushId: String,
        val onionEnabled: Boolean,
        val timelineMode: String,
        val undo: JSONObject? = null
    )

    data class ProjectData(
        val width: Int,
        val height: Int,
        val fps: Float,
        val currentFrameIndex: Int,
        val frames: List<ImageBitmap>,
        val meta: JSONObject = JSONObject(),
        val layerStacks: List<LayerStack> = emptyList()
    )

    suspend fun saveFromFlipbook(
        flipbook: FlipbookBitmapEngine,
        fps: Float,
        meta: JSONObject,
        fileName: String = "project_${System.currentTimeMillis()}"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "projects")
            dir.mkdirs()
            val file = File(dir, if (fileName.endsWith(EXT)) fileName else "$fileName$EXT")
            FileOutputStream(file).use { fos -> writePan2(fos, flipbook, fps, meta) }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToUri(
        uri: Uri,
        flipbook: FlipbookBitmapEngine,
        fps: Float,
        meta: JSONObject
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                writePan2(os, flipbook, fps, meta)
            } ?: return@withContext Result.failure(IllegalStateException("No output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writePan2(
        os: OutputStream,
        flipbook: FlipbookBitmapEngine,
        fps: Float,
        meta: JSONObject
    ) {
        GZIPOutputStream(os).use { gzos ->
            DataOutputStream(gzos).use { out ->
                out.writeBytes(MAGIC_V2)
                out.writeInt(VERSION)
                out.writeInt(flipbook.width)
                out.writeInt(flipbook.height)
                out.writeFloat(fps)
                out.writeInt(flipbook.currentIndex.value)
                val frameList = flipbook.frames.value
                out.writeInt(frameList.size)

                frameList.forEach { frame ->
                    val layers = frame.layers.layers()
                    out.writeInt(layers.size)
                    out.writeInt(frame.layers.activeLayerIndex)
                    layers.forEach { layer ->
                        out.writeUTF(layer.name)
                        out.writeBoolean(layer.visible)
                        out.writeFloat(layer.opacity)
                        val png = pngBytes(layer.bitmap)
                        out.writeInt(png.size)
                        out.write(png)
                    }
                }

                val metaBytes = meta.toString().toByteArray(Charsets.UTF_8)
                out.writeInt(metaBytes.size)
                out.write(metaBytes)
            }
        }
    }

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
                        out.writeBytes(MAGIC_V1)
                        out.writeInt(1)
                        out.writeInt(data.width)
                        out.writeInt(data.height)
                        out.writeFloat(data.fps)
                        out.writeInt(
                            data.currentFrameIndex.coerceIn(
                                0,
                                (data.frames.size - 1).coerceAtLeast(0)
                            )
                        )
                        out.writeInt(data.frames.size)
                        data.frames.forEach { imageBitmap ->
                            val png = pngBytes(imageBitmap)
                            out.writeInt(png.size)
                            out.write(png)
                        }
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
            FileInputStream(file).use { fis -> loadStream(fis) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFromUri(uri: Uri): Result<ProjectData> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { loadStream(it) }
                ?: Result.failure(IllegalStateException("Cannot open uri"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadStream(inputStream: java.io.InputStream): Result<ProjectData> {
        GZIPInputStream(inputStream).use { gzis ->
            DataInputStream(gzis).use { input ->
                val magicBytes = ByteArray(4)
                input.readFully(magicBytes)
                val magic = String(magicBytes, Charsets.US_ASCII)
                return when (magic) {
                    MAGIC_V2 -> loadPan2(input)
                    MAGIC_V1 -> loadPan1(input)
                    else -> Result.failure(
                        IllegalArgumentException("Not a valid .pan (magic=$magic)")
                    )
                }
            }
        }
    }

    private fun loadPan1(input: DataInputStream): Result<ProjectData> {
        val version = input.readInt()
        if (version > 1) {
            return Result.failure(IllegalArgumentException("Unsupported PAN1 version $version"))
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
        return Result.success(
            ProjectData(
                width, height, fps,
                currentFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0)),
                frames, meta
            )
        )
    }

    private fun loadPan2(input: DataInputStream): Result<ProjectData> {
        val version = input.readInt()
        if (version > VERSION) {
            return Result.failure(IllegalArgumentException("Unsupported PAN2 version $version"))
        }
        val width = input.readInt()
        val height = input.readInt()
        val fps = input.readFloat()
        val currentFrameIndex = input.readInt()
        val frameCount = input.readInt()

        val composites = mutableListOf<ImageBitmap>()
        val stacks = mutableListOf<LayerStack>()

        repeat(frameCount) {
            val layerCount = input.readInt()
            val activeIdx = input.readInt()
            val rebuilt = LayerStack(width, height, initialLayers = 1)
            if (layerCount == 0) {
                stacks.add(rebuilt)
                composites.add(rebuilt.composite())
                return@repeat
            }
            for (li in 0 until layerCount) {
                val name = input.readUTF()
                val visible = input.readBoolean()
                val opacity = input.readFloat()
                val pngLen = input.readInt()
                val pngBytes = ByteArray(pngLen)
                input.readFully(pngBytes)
                val bmp = BitmapFactory.decodeByteArray(pngBytes, 0, pngLen)
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                if (li == 0) {
                    rebuilt.replaceActiveBitmap(bmp.asImageBitmap())
                    patchLayerMeta(rebuilt, 0, name, visible, opacity)
                } else {
                    rebuilt.addLayer(name)
                    rebuilt.replaceActiveBitmap(bmp.asImageBitmap())
                    patchLayerMeta(rebuilt, li, name, visible, opacity)
                }
            }
            rebuilt.setActive(activeIdx.coerceIn(0, layerCount - 1))
            stacks.add(rebuilt)
            composites.add(rebuilt.composite())
        }

        val metaLen = input.readInt()
        val meta = if (metaLen > 0) {
            val metaBytes = ByteArray(metaLen)
            input.readFully(metaBytes)
            JSONObject(String(metaBytes, Charsets.UTF_8))
        } else JSONObject()

        return Result.success(
            ProjectData(
                width = width,
                height = height,
                fps = fps,
                currentFrameIndex = currentFrameIndex.coerceIn(
                    0,
                    (composites.size - 1).coerceAtLeast(0)
                ),
                frames = composites,
                meta = meta,
                layerStacks = stacks
            )
        )
    }

    private fun patchLayerMeta(
        stack: LayerStack,
        index: Int,
        name: String,
        visible: Boolean,
        opacity: Float
    ) {
        stack.setActive(index)
        stack.setLayerMeta(index, name, visible, opacity)
    }

    fun listProjects(): List<File> {
        val dir = File(context.filesDir, "projects")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.equals("pan", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteProject(file: File): Boolean {
        return try {
            file.delete()
        } catch (_: Exception) {
            false
        }
    }
}
