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

    @Test
    fun `l'identifiant de la vidéo se lit dans toutes les formes de lien`() {
        listOf(
            "https://youtu.be/QsjP0cZrQdk",
            "https://youtu.be/QsjP0cZrQdk?si=abc123&t=42",
            "https://www.youtube.com/watch?v=QsjP0cZrQdk",
            "https://m.youtube.com/watch?feature=share&v=QsjP0cZrQdk#t=10",
            "https://music.youtube.com/watch?v=QsjP0cZrQdk&list=PL123",
            "https://www.youtube.com/shorts/QsjP0cZrQdk",
            "https://www.youtube.com/embed/QsjP0cZrQdk",
            "youtube.com/live/QsjP0cZrQdk?feature=share",
        ).forEach { assertEquals("QsjP0cZrQdk", YouTubeLink.videoId(it), it) }
        assertEquals("https://i.ytimg.com/vi/QsjP0cZrQdk/hqdefault.jpg", YouTubeLink.thumbnailUrl("https://youtu.be/QsjP0cZrQdk"))
    }

    @Test
    fun `une chaîne, une playlist ou une recherche n'ont pas d'image de vidéo`() {
        listOf(
            "https://www.youtube.com/@hillsong",
            "https://www.youtube.com/playlist?list=PL123",
            "https://www.youtube.com/results?search_query=louange",
            "https://www.youtube.com/watch?list=PL123",
            "https://example.com/watch?v=QsjP0cZrQdk",
        ).forEach { assertNull(YouTubeLink.thumbnailUrl(it), it) }
    }
}
