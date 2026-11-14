package com.xperiencemusic.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeLinkTest {
    @Test
    fun `les liens copiés depuis WhatsApp ou YouTube sont gardés tels quels`() {
        listOf(
            "https://youtu.be/QsjP0cZrQdk?is=9C4SxzGURC87qaDe",
            "https://www.youtube.com/watch?v=ucY6NwQTI3M",
            "https://m.youtube.com/watch?v=BI5Vga_S2XU",
            "https://music.youtube.com/watch?v=abc",
        ).forEach { assertEquals(it, YouTubeLink.normalize("  $it ")) }
    }

    @Test
    fun `un lien collé sans https le reçoit`() {
        assertEquals("https://youtu.be/QsjP0cZrQdk", YouTubeLink.normalize("youtu.be/QsjP0cZrQdk"))
    }

    @Test
    fun `ce qui n'est pas une vidéo YouTube est refusé`() {
        listOf(
            "", "   ", "https://accords.app/JEM414", "youtube", "https://youtu.be", "https://youtu.be/",
            "ftp://youtu.be/x", "https://notyoutube.com/x",
        )
            .forEach { assertNull(YouTubeLink.normalize(it), it) }
    }
}
