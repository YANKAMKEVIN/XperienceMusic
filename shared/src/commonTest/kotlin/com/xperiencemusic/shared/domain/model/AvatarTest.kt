package com.xperiencemusic.shared.domain.model

import com.xperiencemusic.shared.presentation.common.MemberAvatarUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AvatarTest {
    @Test
    fun `un avatar se relit tel qu'il a été enregistré`() {
        val avatars = AvatarPreset.entries.map(Avatar::Preset) + Avatar.Photo("me/abc.jpg")
        avatars.forEach { assertEquals(it, decodeAvatar(it.encode())) }
        assertEquals("preset:BASS_GIRL_1", Avatar.Preset(AvatarPreset.BASS_GIRL_1).encode())
    }

    @Test
    fun `une valeur inconnue montre les initiales plutôt que de planter`() {
        listOf(null, "preset:HARP_GIRL_1", "photo:", "selfie.jpg").forEach { assertNull(decodeAvatar(it)) }
    }

    @Test
    fun `les avatars de son rôle viennent en premier`() {
        val ordered = AvatarPreset.orderedFor(Instrument.DRUMS)
        assertEquals(AvatarPreset.entries.size, ordered.size)
        assertEquals(List(4) { Instrument.DRUMS }, ordered.take(4).map { it.instrument })
    }

    @Test
    fun `les initiales prennent les deux premiers mots`() {
        assertEquals("GM", MemberAvatarUi("Grâce Mbala", null).initials)
        assertEquals("JP", MemberAvatarUi("jean-pierre", null).initials)
        assertEquals("R", MemberAvatarUi(" Ruth ", null).initials)
    }
}
