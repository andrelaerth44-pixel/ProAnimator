package com.proanimator.core.export

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * H.264 (default) or H.265/HEVC when [useHevc] and device supports it.
 */
class Mp4Encoder(private val context: Context) {

    companion object {
        private const val TAG = "Mp4Encoder"
        private const val MIME_AVC = "video/avc"
        private const val MIME_HEVC = "video/hevc"
        private const val TIMEOUT_US = 10_000L
        private const val BIT_RATE = 4_000_000
        private const val I_FRAME_INTERVAL = 1
    }

    data class Progress(val current: Int, val total: Int, val message: String)

    suspend fun encode(
        frames: List<ImageBitmap>,
        width: Int,
        height: Int,
        fps: Float = 24f,
        useHevc: Boolean = false,
        onProgress: (Progress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (frames.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No frames"))
        }

        val w = width and 0x7FFFFFFE
        val h = height and 0x7FFFFFFE
        val mime = if (useHevc && supportsMime(MIME_HEVC)) MIME_HEVC else MIME_AVC

        val outputFile = File(
            context.getExternalFilesDir(null),
            "exports/proanimator_${System.currentTimeMillis()}.mp4"
        )
        outputFile.parentFile?.mkdirs()

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            onProgress(Progress(0, frames.size, "Encoder $mime…"))

            val colorFormat = selectColorFormat(mime)
            val format = MediaFormat.createVideoFormat(mime, w, h).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps.toInt().coerceIn(1, 60))
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            codec = MediaCodec.createEncoderByType(mime)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var inputIndex = 0
            var outputDone = false
            var inputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        if (inputIndex >= frames.size) {
                            codec.queueInputBuffer(
                                inIdx, 0, 0,
                                computePts(inputIndex, fps),
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val inputBuffer = codec.getInputBuffer(inIdx)!!
                            inputBuffer.clear()
                            val bitmap = frames[inputIndex].asAndroidBitmap()
                            val scaled = if (bitmap.width != w || bitmap.height != h) {
                                Bitmap.createScaledBitmap(bitmap, w, h, true)
                            } else bitmap
                            val yuv = bitmapToYuv420(scaled, w, h, colorFormat)
                            inputBuffer.put(yuv)
                            codec.queueInputBuffer(
                                inIdx, 0, yuv.size,
                                computePts(inputIndex, fps), 0
                            )
                            if (scaled !== bitmap) scaled.recycle()
                            onProgress(
                                Progress(
                                    inputIndex + 1, frames.size,
                                    "Encoding ${inputIndex + 1}/${frames.size}"
                                )
                            )
                            inputIndex++
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw RuntimeException("Format changed twice")
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outIdx >= 0 -> {
                        val encoded = codec.getOutputBuffer(outIdx)!!
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted) {
                            encoded.position(bufferInfo.offset)
                            encoded.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            onProgress(Progress(frames.size, frames.size, "MP4 ready ($mime)"))
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Encode failed", e)
            outputFile.delete()
            Result.failure(e)
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun supportsMime(mime: String): Boolean {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, true) }
        }
    }

    private fun computePts(frameIndex: Int, fps: Float): Long =
        (frameIndex * 1_000_000L / fps).toLong()

    private fun selectColorFormat(mime: String): Int {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            try {
                if (!info.supportedTypes.any { it.equals(mime, true) }) continue
                val caps = info.getCapabilitiesForType(mime)
                if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar))
                    return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar))
                    return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                if (caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible))
                    return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            } catch (_: Exception) {}
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    }

    private fun bitmapToYuv420(
        bitmap: Bitmap, width: Int, height: Int, colorFormat: Int
    ): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val yuv = ByteArray(width * height * 3 / 2)
        val isSemiPlanar =
            colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar ||
                colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        var yIndex = 0
        var uvIndex = width * height
        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    if (isSemiPlanar) {
                        if (uvIndex < yuv.size - 1) {
                            yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                            yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                        }
                    } else {
                        val uvPlaneSize = width * height / 4
                        val uIndex = width * height + (j / 2) * (width / 2) + (i / 2)
                        val vIndex = width * height + uvPlaneSize + (j / 2) * (width / 2) + (i / 2)
                        if (uIndex < yuv.size) yuv[uIndex] = u.coerceIn(0, 255).toByte()
                        if (vIndex < yuv.size) yuv[vIndex] = v.coerceIn(0, 255).toByte()
                    }
                }
            }
        }
        return yuv
    }
}
