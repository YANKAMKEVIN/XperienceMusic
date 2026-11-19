package com.xperiencemusic.shared.presentation.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHighlightTest {
    @Test
    fun `les lettres trouvées sont situées sans tenir compte des accents ni de la casse`() {
        // The search trims what was typed: so does the highlight.
        assertEquals(listOf(2..4), searchMatches("À Toi la gloire", "toi "))
        assertEquals(listOf(0..3), searchMatches("Élève ta voix", "elev"))
        assertEquals(listOf(26..32), searchMatches("Saint, saint, saint est l’Éternel", "ÉTERNEL"))
    }

    @Test
    fun `chaque occurrence est située, sans chevauchement`() {
        assertEquals(listOf(0..1, 2..3, 4..5), searchMatches("lalala", "la"))
        assertEquals(listOf(0..4, 7..11, 14..18), searchMatches("Saint, saint, saint", "saint"))
        assertEquals(listOf(0..2), searchMatches("aaaa", "aaa"))
    }

    @Test
    fun `sans recherche ou sans correspondance, rien n'est surligné`() {
        assertTrue(searchMatches("À toi la gloire", "  ").isEmpty())
        assertTrue(searchMatches("À toi la gloire", "alléluia").isEmpty())
    }

    @Test
    fun `une apostrophe tapée trouve l'apostrophe typographique`() {
        assertEquals(listOf(8..14), searchMatches("Louange l’agneau", "l'agnea"))
    }

    @Test
    fun `une ligne longue commence quelques mots avant le mot trouvé`() {
        val line = "Dans les cieux et sur la terre, il n’est aucun nom plus glorieux"
        val excerpt = lyricExcerpt(line, "glorieux")
        assertTrue(excerpt.startsWith("…"))
        assertTrue(excerpt.endsWith("plus glorieux"))
        assertTrue(searchMatches(excerpt, "glorieux").single().first < 30)
        // The excerpt opens on a whole word.
        assertTrue(line.contains(" " + excerpt.removePrefix("…")))
    }

    @Test
    fun `une ligne dont le mot est déjà visible reste entière`() {
        assertEquals("Gloire à toi, Seigneur", lyricExcerpt("Gloire à toi, Seigneur", "seigneur"))
        assertEquals("Gloire à toi", lyricExcerpt("Gloire à toi", "absent"))
    }
}
