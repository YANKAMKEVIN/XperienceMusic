package com.xperiencemusic.shared.ui.platform

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Re-encodes any audio file the phone can decode into mono AAC in an MP4 container, streaming from
 * [uri] so a long rehearsal never sits whole in memory. Returns false when the file cannot be
 * converted; [out] is then left incomplete and should be ignored.
 */
internal object AacTranscoder {
    private const val AAC = MediaFormat.MIMETYPE_AUDIO_AAC
    private const val TIMEOUT_US = 10_000L
    private const val MAX_AAC_SAMPLE_RATE = 48_000
    private const val ENCODER_INPUT_BYTES = 64 * 1024
    private const val BYTES_PER_SAMPLE = 2
    private const val MICROS_PER_SECOND = 1_000_000L

    fun transcode(context: Context, uri: Uri, out: File, bitrate: Int, onProgress: (Float) -> Unit): Boolean {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var pipeline: Pipeline? = null
        return try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return false
            extractor.selectTrack(track)
            val input = extractor.getTrackFormat(track)
            val durationUs = if (input.containsKey(MediaFormat.KEY_DURATION)) input.getLong(MediaFormat.KEY_DURATION) else 0L
            decoder = MediaCodec.createDecoderByType(input.getString(MediaFormat.KEY_MIME)!!).apply {
                configure(input, null, null, 0)
                start()
            }
            muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            Pipeline(extractor, decoder, muxer, bitrate, durationUs, onProgress).also { pipeline = it }.run()
        } catch (e: java.io.IOException) {
            failed(e)
        } catch (e: IllegalStateException) {
            failed(e)
        } catch (e: IllegalArgumentException) {
            failed(e)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { pipeline?.encoder?.stop() }
            runCatching { pipeline?.encoder?.release() }
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            extractor.release()
        }
    }

    /** The caller then uploads the original file: worth a log line, not a crash. */
    private fun failed(cause: Exception): Boolean {
        Log.w(TAG, "Could not compress the audio file", cause)
        return false
    }

    /** Decoder → mono 16-bit PCM → AAC encoder → MP4 muxer, all driven from one loop. */
    private class Pipeline(
        private val extractor: MediaExtractor,
        private val decoder: MediaCodec,
        private val muxer: MediaMuxer,
        private val bitrate: Int,
        private val durationUs: Long,
        private val onProgress: (Float) -> Unit,
    ) {
        var encoder: MediaCodec? = null
            private set
        private var finished = false

        private val info = MediaCodec.BufferInfo()
        private val pending = ArrayDeque<ByteBuffer>()
        private var pendingBytes = 0
        private var sampleRate = 0
        private var channels = 0
        private var isFloat = false
        private var extractorDone = false
        private var decoderDone = false
        private var encoderInputDone = false
        private var muxerTrack = -1
        private var samplesQueued = 0L
        private var lastProgress = -1

        /** True once the whole file went through and the muxer has its track. */
        fun run(): Boolean {
            while (!finished) {
                // The encoder is slower than the decoder: hold the decoder back instead of piling PCM up.
                val hasRoom = pendingBytes < MAX_PENDING_BYTES
                if (!extractorDone && hasRoom) feedDecoder()
                if (!decoderDone && hasRoom) drainDecoder()
                encoder?.let { enc ->
                    if (!encoderInputDone) feedEncoder(enc)
                    drainEncoder(enc)
                }
                // The decoder ended before ever telling its format: nothing was decoded.
                if (decoderDone && encoder == null) return false
            }
            return true
        }

        private fun feedDecoder() {
            val index = decoder.dequeueInputBuffer(TIMEOUT_US)
            if (index < 0) return
            val buffer = decoder.getInputBuffer(index) ?: return
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) {
                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                extractorDone = true
            } else {
                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                reportProgress(extractor.sampleTime)
                extractor.advance()
            }
        }

        private fun drainDecoder() {
            val index = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> startEncoder(decoder.outputFormat)
                index >= 0 -> {
                    val buffer = decoder.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {
                        if (encoder == null) startEncoder(decoder.outputFormat)
                        buffer.position(info.offset).limit(info.offset + info.size)
                        val mono = toMono16(buffer.slice().order(ByteOrder.nativeOrder()))
                        pending.addLast(mono)
                        pendingBytes += mono.remaining()
                    }
                    decoder.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) decoderDone = true
                }
            }
        }

        private fun startEncoder(format: MediaFormat) {
            if (encoder != null) return
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            isFloat = format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
            require(sampleRate in 1..MAX_AAC_SAMPLE_RATE) { "Unsupported sample rate $sampleRate" }
            val output = MediaFormat.createAudioFormat(AAC, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, ENCODER_INPUT_BYTES)
            }
            encoder = MediaCodec.createEncoderByType(AAC).apply {
                configure(output, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
        }

        /** Averages the channels of each frame into one 16-bit sample. */
        private fun toMono16(source: ByteBuffer): ByteBuffer {
            val bytesPerFrame = (if (isFloat) Float.SIZE_BYTES else Short.SIZE_BYTES) * channels
            val frames = source.remaining() / bytesPerFrame
            val mono = ByteBuffer.allocate(frames * BYTES_PER_SAMPLE).order(ByteOrder.nativeOrder())
            repeat(frames) {
                var sum = 0f
                repeat(channels) { sum += if (isFloat) source.float * Short.MAX_VALUE else source.short.toFloat() }
                val average = (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                mono.putShort(average.toShort())
            }
            mono.flip()
            return mono
        }

        private fun feedEncoder(enc: MediaCodec) {
            if (pending.isEmpty() && !decoderDone) return
            val index = enc.dequeueInputBuffer(TIMEOUT_US)
            if (index < 0) return
            val buffer = enc.getInputBuffer(index) ?: return
            buffer.clear()
            while (buffer.hasRemaining() && pending.isNotEmpty()) {
                val chunk = pending.first()
                val count = minOf(buffer.remaining(), chunk.remaining())
                val slice = chunk.slice().also { it.limit(count) }
                buffer.put(slice)
                chunk.position(chunk.position() + count)
                pendingBytes -= count
                if (!chunk.hasRemaining()) pending.removeFirst()
            }
            val bytes = buffer.position()
            val timeUs = samplesQueued * MICROS_PER_SECOND / sampleRate
            samplesQueued += bytes / BYTES_PER_SAMPLE
            val isLast = decoderDone && pending.isEmpty()
            enc.queueInputBuffer(index, 0, bytes, timeUs, if (isLast) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
            if (isLast) encoderInputDone = true
        }

        private fun drainEncoder(enc: MediaCodec) {
            val index = enc.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    muxerTrack = muxer.addTrack(enc.outputFormat)
                    muxer.start()
                }
                index >= 0 -> {
                    val buffer = enc.getOutputBuffer(index)
                    val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    val hasAudio = buffer != null && info.size > 0 && !isConfig
                    if (hasAudio && muxerTrack >= 0) {
                        buffer.position(info.offset).limit(info.offset + info.size)
                        muxer.writeSampleData(muxerTrack, buffer!!, info)
                    }
                    enc.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        finished = muxerTrack >= 0
                        if (!finished) error("The encoder ended without output")
                    }
                }
            }
        }

        private fun reportProgress(timeUs: Long) {
            if (durationUs <= 0) return
            val percent = (timeUs * PERCENT / durationUs).toInt().coerceIn(0, PERCENT)
            if (percent != lastProgress) {
                lastProgress = percent
                onProgress(percent / PERCENT.toFloat())
            }
        }
    }

    private const val PERCENT = 100
    private const val TAG = "AacTranscoder"
    private const val MAX_PENDING_BYTES = 1024 * 1024
}
