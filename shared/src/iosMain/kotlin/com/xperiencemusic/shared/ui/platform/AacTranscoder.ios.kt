package com.xperiencemusic.shared.ui.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVLinearPCMIsNonInterleaved
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFoundation.AVAssetReader
import platform.AVFoundation.AVAssetReaderStatusCompleted
import platform.AVFoundation.AVAssetReaderTrackOutput
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.tracksWithMediaType
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.CoreFoundation.CFRelease
import platform.CoreMedia.CMSampleBufferGetPresentationTimeStamp
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.usleep

/**
 * Re-encodes [source] into mono AAC at [bitrate] in an M4A file at [out], reading and writing in
 * chunks so a long rehearsal never sits whole in memory. Blocks the calling thread: run it off the
 * main one. Returns false when the file cannot be converted.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun transcodeToAac(source: NSURL, out: NSURL, bitrate: Int, onProgress: (Float) -> Unit): Boolean {
    NSFileManager.defaultManager.removeItemAtURL(out, error = null)
    val asset = AVURLAsset.URLAssetWithURL(source, options = null)
    val reading = openReader(asset)
    val writing = openWriter(out, bitrate)
    val started = reading != null && writing != null &&
        reading.first.startReading() && writing.first.startWriting()
    if (!started) return false
    val (reader, output) = reading!!
    val (writer, input) = writing!!
    writer.startSessionAtSourceTime(CMTimeMake(value = 0, timescale = 1))
    val total = CMTimeGetSeconds(asset.duration).takeIf { it.isFinite() && it > 0 }
    val copied = copySamples(output, input, total, onProgress) && reader.status == AVAssetReaderStatusCompleted
    if (!copied) {
        reader.cancelReading()
        writer.cancelWriting()
        return false
    }
    input.markAsFinished()
    val done = dispatch_semaphore_create(0)
    writer.finishWritingWithCompletionHandler { dispatch_semaphore_signal(done) }
    dispatch_semaphore_wait(done, DISPATCH_TIME_FOREVER)
    return writer.status == AVAssetWriterStatusCompleted
}

/** The first audio track, decoded to mono 16-bit PCM at [SAMPLE_RATE]. */
@OptIn(ExperimentalForeignApi::class)
private fun openReader(asset: AVURLAsset): Pair<AVAssetReader, AVAssetReaderTrackOutput>? {
    val track = asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack
    val reader = track?.let { AVAssetReader.assetReaderWithAsset(asset, error = null) }
    val output = track?.let {
        AVAssetReaderTrackOutput.assetReaderTrackOutputWithTrack(
            it,
            outputSettings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatLinearPCM.toInt(),
                AVLinearPCMBitDepthKey to PCM_BITS,
                AVLinearPCMIsFloatKey to false,
                AVLinearPCMIsBigEndianKey to false,
                AVLinearPCMIsNonInterleaved to false,
                AVNumberOfChannelsKey to 1,
                AVSampleRateKey to SAMPLE_RATE,
            ),
        ).apply { alwaysCopiesSampleData = false }
    }
    if (reader == null || output == null || !reader.canAddOutput(output)) return null
    reader.addOutput(output)
    return reader to output
}

/** An M4A file holding mono AAC at [bitrate]. */
@OptIn(ExperimentalForeignApi::class)
private fun openWriter(out: NSURL, bitrate: Int): Pair<AVAssetWriter, AVAssetWriterInput>? {
    val writer = AVAssetWriter.assetWriterWithURL(out, fileType = AVFileTypeAppleM4A, error = null) ?: return null
    val input = AVAssetWriterInput.assetWriterInputWithMediaType(
        AVMediaTypeAudio,
        outputSettings = mapOf<Any?, Any?>(
            AVFormatIDKey to kAudioFormatMPEG4AAC.toInt(),
            AVNumberOfChannelsKey to 1,
            AVSampleRateKey to SAMPLE_RATE,
            AVEncoderBitRateKey to bitrate,
        ),
    )
    input.expectsMediaDataInRealTime = false
    if (!writer.canAddInput(input)) return null
    writer.addInput(input)
    return writer to input
}

/** Moves every decoded buffer to the encoder; false when the encoder refused one. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun copySamples(
    output: AVAssetReaderTrackOutput,
    input: AVAssetWriterInput,
    totalSeconds: Double?,
    onProgress: (Float) -> Unit,
): Boolean {
    var lastPercent = -1
    while (true) {
        while (!input.readyForMoreMediaData) usleep(WAIT_MICROS)
        val buffer = output.copyNextSampleBuffer() ?: return true
        val seconds = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(buffer))
        val appended = input.appendSampleBuffer(buffer)
        CFRelease(buffer)
        if (!appended) return false
        val percent = totalSeconds?.let { (seconds / it * PERCENT).toInt().coerceIn(0, PERCENT) }
        if (percent != null && percent != lastPercent) {
            lastPercent = percent
            onProgress(percent / PERCENT.toFloat())
        }
    }
}

private const val PCM_BITS = 16
private const val SAMPLE_RATE = 44_100.0
private const val WAIT_MICROS = 2_000u
private const val PERCENT = 100
