package com.xperiencemusic.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioCompressionTest {
    private val mb = 1024L * 1024

    @Test
    fun `un petit fichier part tel quel`() {
        assertNull(AudioCompression.targetBitrate(4 * mb, 180))
    }

    @Test
    fun `une répétition d'une heure du dictaphone est allégée à 64 kbit par seconde`() {
        assertEquals(AudioCompression.TARGET_BITRATE, AudioCompression.targetBitrate(120 * mb, 3600))
    }

    @Test
    fun `une très longue répétition baisse le débit pour tenir sous la limite`() {
        val bitrate = AudioCompression.targetBitrate(300 * mb, 3 * 3600)!!
        assertEquals(true, bitrate < AudioCompression.TARGET_BITRATE)
        assertEquals(true, bitrate.toLong() * 3 * 3600 / 8 <= 45 * mb)
    }

    @Test
    fun `le débit ne descend jamais sous le minimum audible`() {
        assertEquals(AudioCompression.MIN_BITRATE, AudioCompression.targetBitrate(900 * mb, 10 * 3600))
    }

    @Test
    fun `un fichier déjà léger pour sa durée n'est pas recompressé`() {
        // 30 minutes at 64 kbit/s: 14 MB, above the size threshold but already at the target.
        assertNull(AudioCompression.targetBitrate(15 * mb, 1800))
    }

    @Test
    fun `sans durée connue, un gros fichier est allégé au débit cible`() {
        assertEquals(AudioCompression.TARGET_BITRATE, AudioCompression.targetBitrate(60 * mb, null))
    }
}
