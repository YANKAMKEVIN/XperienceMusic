package com.xperiencemusic.music.chart

import com.xperiencemusic.music.theory.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordsOverLyricsTest {
    private val doMajeur = Key(0)

    @Test
    fun `place chaque accord sur la syllabe qu'il surplombe`() {
        val source = """
            Do          Sol
            Tu es saint, Seigneur
        """.trimIndent()
        assertEquals("[Do]Tu es saint,[Sol] Seigneur", ChordsOverLyrics.toInline(source, doMajeur))
    }

    @Test
    fun `accepte les lettres les chiffres et les accords enrichis`() {
        val source = "C    Am/E   F7M\nGloire à toi, ô Dieu"
        assertEquals("[C]Gloir[Am/E]e à toi[F7M], ô Dieu", ChordsOverLyrics.toInline(source, doMajeur))
        assertEquals("[1]Amen [5]amen", ChordsOverLyrics.toInline("1    5\nAmen amen", doMajeur))
    }

    @Test
    fun `un accord après la fin des paroles reste en bout de ligne`() {
        assertEquals("[Do]Amen     [Sol]", ChordsOverLyrics.toInline("Do       Sol\nAmen", doMajeur))
    }

    @Test
    fun `une ligne d'accords sans paroles reste une ligne d'accords`() {
        val source = "Intro :\nDo | Sol | Lam | Fa\n\nRefrain\nDo\nGloire"
        assertEquals("# Intro\n[Do] [Sol] [Lam] [Fa]\n\n# Refrain\n[Do]Gloire", ChordsOverLyrics.toInline(source, doMajeur))
    }

    @Test
    fun `reconnaît les titres de section`() {
        assertEquals("# Couplet 2", ChordsOverLyrics.toInline("[Couplet 2]", doMajeur))
        assertEquals("# Pont", ChordsOverLyrics.toInline("PONT :", doMajeur))
    }

    @Test
    fun `des paroles en minuscules ne sont pas prises pour des accords`() {
        val source = "Sol\nla la la, alléluia"
        assertEquals("[Sol]la la la, alléluia", ChordsOverLyrics.toInline(source, doMajeur))
        assertFalse(ChordsOverLyrics.applies("la la la\nalléluia", doMajeur))
    }

    @Test
    fun `une grille déjà au format de l'app ne change pas`() {
        val chart = "# Refrain\n[Do]Tu es [Sol]saint"
        assertFalse(ChordsOverLyrics.applies(chart, doMajeur))
        assertEquals(chart, ChordsOverLyrics.toInline(chart, doMajeur))
    }

    @Test
    fun `repère une grille avec les accords au-dessus`() {
        assertTrue(ChordsOverLyrics.applies("Do   Sol\nTu es saint", doMajeur))
    }
}
