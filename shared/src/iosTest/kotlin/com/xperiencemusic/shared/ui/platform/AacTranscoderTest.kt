package com.xperiencemusic.shared.ui.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class AacTranscoderTest {
    @Test
    fun `une répétition stéréo en WAV devient un M4A mono bien plus léger`() {
        val seconds = 120
        val source = NSURL.fileURLWithPath(NSTemporaryDirectory() + "rehearsal-test.wav")
        val out = NSURL.fileURLWithPath(NSTemporaryDirectory() + "rehearsal-test.m4a")
        writeStereoWav(source, seconds)
        val progress = mutableListOf<Float>()

        assertTrue(transcodeToAac(source, out, bitrate = 64_000) { progress += it })

        val before = size(source)
        val after = size(out)
        // 2 minutes at 64 kbit/s is about 1 MB, against 21 MB of 16-bit stereo PCM.
        assertTrue(after in 500_000..1_500_000, "compressed to $after bytes from $before")
        assertTrue(progress.isNotEmpty() && progress.last() > 0.9f, "progress $progress")
        assertEquals(progress.sorted(), progress)
        NSFileManager.defaultManager.removeItemAtURL(source, error = null)
        NSFileManager.defaultManager.removeItemAtURL(out, error = null)
    }

    private fun size(url: NSURL): Long =
        (NSFileManager.defaultManager.attributesOfItemAtPath(url.path!!, error = null)!![NSFileSize] as NSNumber).longLongValue

    /** A 440 Hz tone on the left channel and 660 Hz on the right, 44.1 kHz, 16 bit. */
    private fun writeStereoWav(url: NSURL, seconds: Int) {
        val rate = 44_100
        val frames = rate * seconds
        val dataBytes = frames * 4
        val bytes = ByteArray(44 + dataBytes)
        fun put32(at: Int, v: Int) = repeat(4) { bytes[at + it] = (v shr (8 * it)).toByte() }
        fun put16(at: Int, v: Int) = repeat(2) { bytes[at + it] = (v shr (8 * it)).toByte() }
        "RIFF".encodeToByteArray().copyInto(bytes, 0)
        put32(4, 36 + dataBytes)
        "WAVEfmt ".encodeToByteArray().copyInto(bytes, 8)
        put32(16, 16); put16(20, 1); put16(22, 2); put32(24, rate); put32(28, rate * 4); put16(32, 4); put16(34, 16)
        "data".encodeToByteArray().copyInto(bytes, 36)
        put32(40, dataBytes)
        for (i in 0 until frames) {
            val t = i.toDouble() / rate
            put16(44 + i * 4, (sin(2 * PI * 440 * t) * 8000).toInt())
            put16(46 + i * 4, (sin(2 * PI * 660 * t) * 8000).toInt())
        }
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        data.writeToURL(url, atomically = true)
    }
}
