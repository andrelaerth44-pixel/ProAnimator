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
 * Mux H.264/H.265 video with audio.
 * Non-AAC sources are transcoded via [AacTranscoder] first.
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

        // Transcode MP3/etc → AAC if needed
        val aacFile = AacTranscoder.ensureAac(context, audioUri)
        val audioSource: Any = aacFile ?: audioUri

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            when (audioSource) {
                is File -> audioExtractor.setDataSource(audioSource.absolutePath)
                is Uri -> audioExtractor.setDataSource(context, audioSource, null)
            }

            val videoTrack = selectTrack(videoExtractor, "video/")
            val audioTrack = selectTrack(audioExtractor, "audio/")
            if (videoTrack < 0) {
                return Result.failure(IllegalStateException("No video track"))
            }

            videoExtractor.selectTrack(videoTrack)
            val videoFormat = videoExtractor.getTrackFormat(videoTrack)

            var audioFormat: MediaFormat? = null
            var canCopyAudio = false
            if (audioTrack >= 0) {
                audioExtractor.selectTrack(audioTrack)
                audioFormat = audioExtractor.getTrackFormat(audioTrack)
                val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
                canCopyAudio = mime.contains("mp4a") || mime.contains("aac")
            }

            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideo = muxer.addTrack(videoFormat)
            val outAudio = if (canCopyAudio && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else {
                if (audioFormat != null) Log.w(TAG, "Audio not AAC after transcode — video only")
                -1
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()

            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            while (true) {
                val size = videoExtractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.set(0, size, videoExtractor.sampleTime, videoExtractor.sampleFlags)
                muxer.writeSampleData(outVideo, buffer, info)
                videoExtractor.advance()
            }

            if (outAudio >= 0) {
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    val size = audioExtractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.set(0, size, audioExtractor.sampleTime, audioExtractor.sampleFlags)
                    muxer.writeSampleData(outAudio, buffer, info)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null

            aacFile?.delete()
            return Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Mux failed", e)
            outputFile.delete()
            aacFile?.delete()
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
