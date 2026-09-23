package com.proanimator.core.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Transcode any supported audio (MP3, etc.) → AAC LC for MediaMuxer MP4.
 * Decode → PCM → AAC encode → temp .m4a, then usable by [AudioVideoMuxer].
 */
object AacTranscoder {

    private const val TAG = "AacTranscoder"
    private const val TIMEOUT_US = 10_000L
    private const val AAC_MIME = "audio/mp4a-latm"
    private const val BIT_RATE = 128_000

    /**
     * @return File with AAC track, or null if already AAC / failure
     */
    fun ensureAac(context: Context, audioUri: Uri): File? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, audioUri, null)
            val track = selectAudio(extractor) ?: return null
            extractor.selectTrack(track)
            val inFormat = extractor.getTrackFormat(track)
            val mime = inFormat.getString(MediaFormat.KEY_MIME) ?: return null

            if (mime.contains("mp4a") || mime.contains("aac")) {
                // Already AAC — copy raw file not needed; muxer can read URI
                return null // signal: use original
            }

            val sampleRate = inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val outFile = File(
                context.cacheDir,
                "aac_${System.currentTimeMillis()}.m4a"
            )

            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inFormat, null, null, 0)
            decoder.start()

            val aacFormat = MediaFormat.createAudioFormat(AAC_MIME, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            val encoder = MediaCodec.createEncoderByType(AAC_MIME)
            encoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxTrack = -1
            var muxStarted = false

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                // Feed decoder from extractor
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(
                                inIdx, 0, sampleSize, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                // Drain decoder → encoder
                val decOut = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                if (decOut >= 0) {
                    val pcm = decoder.getOutputBuffer(decOut)!!
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        var remaining = info.size
                        var offset = info.offset
                        while (remaining > 0) {
                            val encIn = encoder.dequeueInputBuffer(TIMEOUT_US)
                            if (encIn < 0) break
                            val encBuf = encoder.getInputBuffer(encIn)!!
                            encBuf.clear()
                            val chunk = minOf(remaining, encBuf.capacity())
                            pcm.position(offset)
                            pcm.limit(offset + chunk)
                            encBuf.put(pcm)
                            encoder.queueInputBuffer(
                                encIn, 0, chunk, info.presentationTimeUs, 0
                            )
                            offset += chunk
                            remaining -= chunk
                        }
                    }
                    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    decoder.releaseOutputBuffer(decOut, false)
                    if (eos) {
                        val encIn = encoder.dequeueInputBuffer(TIMEOUT_US)
                        if (encIn >= 0) {
                            encoder.queueInputBuffer(
                                encIn, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        }
                    }
                }

                // Drain encoder → muxer
                val encOut = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    encOut == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxStarted) {
                            muxTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxStarted = true
                        }
                    }
                    encOut >= 0 -> {
                        val encoded = encoder.getOutputBuffer(encOut)!!
                        if (info.size > 0 && muxStarted &&
                            info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            encoded.position(info.offset)
                            encoded.limit(info.offset + info.size)
                            muxer.writeSampleData(muxTrack, encoded, info)
                        }
                        encoder.releaseOutputBuffer(encOut, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            if (muxStarted) muxer.stop()
            muxer.release()
            decoder.stop(); decoder.release()
            encoder.stop(); encoder.release()

            return if (outFile.exists() && outFile.length() > 0) outFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Transcode failed", e)
            return null
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    private fun selectAudio(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }
}
