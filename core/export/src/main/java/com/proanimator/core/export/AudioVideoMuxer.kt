package com.proanimator.core.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Mux pre-encoded video MP4 with an audio source (AAC preferred).
 *
 * Strategy:
 * 1. If audio track is already AAC → copy samples into muxer (fast path)
 * 2. Otherwise skip audio and return video-only (log warning)
 *
 * Full PCM→AAC re-encode can be added later without changing this API.
 */
object AudioVideoMuxer {

    private const val TAG = "AudioVideoMuxer"

    fun mux(
        context: Context,
        videoFile: File,
        audioUri: Uri?,
        outputFile: File
    ): Result<File> {
        if (audioUri == null || !videoFile.exists()) {
            return Result.success(videoFile)
        }

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            try {
                audioExtractor.setDataSource(context, audioUri, null)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot open audio", e)
                return Result.success(videoFile)
            }

            val videoTrack = selectTrack(videoExtractor, "video/")
            val audioTrack = selectTrack(audioExtractor, "audio/")
            if (videoTrack < 0) {
                return Result.failure(IllegalStateException("No video track"))
            }

            videoExtractor.selectTrack(videoTrack)
            val videoFormat = videoExtractor.getTrackFormat(videoTrack)

            var audioFormat: MediaFormat? = null
            var audioMime: String? = null
            if (audioTrack >= 0) {
                audioExtractor.selectTrack(audioTrack)
                audioFormat = audioExtractor.getTrackFormat(audioTrack)
                audioMime = audioFormat.getString(MediaFormat.KEY_MIME)
            }

            // Only remux AAC (MediaMuxer MP4 requirement for simple path)
            val canCopyAudio = audioMime != null && (
                audioMime.contains("mp4a") || audioMime.contains("aac")
            )

            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideo = muxer.addTrack(videoFormat)
            val outAudio = if (canCopyAudio && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else {
                if (audioMime != null) Log.w(TAG, "Audio mime $audioMime not AAC — video only")
                -1
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()

            // Copy video
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            while (true) {
                val size = videoExtractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = videoExtractor.sampleTime
                info.flags = videoExtractor.sampleFlags
                muxer.writeSampleData(outVideo, buffer, info)
                videoExtractor.advance()
            }

            // Copy audio if compatible
            if (outAudio >= 0) {
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    val size = audioExtractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = audioExtractor.sampleTime
                    info.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(outAudio, buffer, info)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null

            return Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Mux failed", e)
            outputFile.delete()
            return Result.failure(e)
        } finally {
            try { videoExtractor.release() } catch (_: Exception) {}
            try { audioExtractor.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun selectTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) return i
        }
        return -1
    }
}
