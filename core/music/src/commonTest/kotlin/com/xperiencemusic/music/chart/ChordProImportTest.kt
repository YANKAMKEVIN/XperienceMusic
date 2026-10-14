package com.xperiencemusic.music.chart

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChordProImportTest {
    private val source = """
        {t: Chant d'essai}
        {st: Auteur Imaginaire}
        {n: JEM999}
        {key: Em}
        {tags: louange}

        {c: © 2001 Éditions Exemple/LTC}
        {c: recueil – JEM999}

        {c: Strophe a}
        {start_of_verse}
        [Em]Une ligne [Am]inventée
        [Am/F#]pour le [B7]test
        {end_of_verse}
        {start_of_chorus}
        Re[Em]frain [D7][G]
        {end_of_chorus}
    """.trimIndent()

    @Test
    fun `lit le titre l'auteur et la tonalité`() {
        val song = ChordProImport.parse(source)
        assertEquals("Chant d'essai", song.title)
        assertEquals("Auteur Imaginaire", song.artist)
        assertEquals(Key(4, Mode.MINOR), song.key)
    }

    @Test
    fun `garde les commentaires hors section comme crédits`() {
        assertEquals(listOf("© 2001 Éditions Exemple/LTC", "recueil – JEM999"), ChordProImport.parse(source).credits)
    }

    @Test
    fun `transforme les blocs en sections de la grille`() {
        assertEquals(
            """
            # Strophe a
            [Em]Une ligne [Am]inventée
            [Am/F#]pour le [B7]test
            # Refrain
            Re[Em]frain [D7][G]
            """.trimIndent(),
            ChordProImport.parse(source).chart,
        )
    }

    @Test
    fun `un refrain déjà titré ne reçoit pas de second titre`() {
        val chart = ChordProImport.parse("{c: Refrain}\n{soc}\n[C]Gloire\n{eoc}").chart
        assertEquals("# Refrain\n[C]Gloire", chart)
    }

    @Test
    fun `les accords importés sont tous compris par la grille`() {
        val song = ChordProImport.parse(source)
        assertTrue(ChordChart.unknownChords(song.chart, song.key!!).isEmpty())
    }

    @Test
    fun `une tonalité illisible reste vide`() {
        assertNull(ChordProImport.parse("{key: ???}\n[C]Gloire").key)
    }
}
