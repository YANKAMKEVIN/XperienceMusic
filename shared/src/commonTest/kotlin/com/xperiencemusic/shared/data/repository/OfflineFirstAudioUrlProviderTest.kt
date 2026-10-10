package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.fakes.FakeAudioCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineFirstAudioUrlProviderTest {
    @Test
    fun `le fichier téléchargé passe avant le réseau`() = runTest {
        val provider = OfflineFirstAudioUrlProvider(FakeAudioCache(setOf("songs/a.m4a"))) { "https://signed/$it" }
        assertEquals("file:///songs/a.m4a", provider.playableUrl("songs/a.m4a"))
        assertEquals("https://signed/songs/b.m4a", provider.playableUrl("songs/b.m4a"))
    }
}
