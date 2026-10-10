package com.xperiencemusic.music.chart

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Notation
import kotlin.test.Test
import kotlin.test.assertEquals

class ChordChartTest {
    private val doMajeur = Key(0)

    @Test
    fun `normalize stocke les accords en chiffres Nashville`() {
        val source = "[Do]Tu es [Sol]saint, [Lam]Sei[Fa]gneur [N.C.]"
        assertEquals("[1]Tu es [5]saint, [6m]Sei[4]gneur [N.C.]", ChordChart.normalize(source, doMajeur))
    }

    @Test
    fun `convert réécrit les accords dans une autre notation`() {
        assertEquals("[Do]Gloire [Lam]à toi", ChordChart.convert("[1]Gloire [6m]à toi", doMajeur, Notation.SOLFEGE))
        assertEquals("[C]Gloire [Am]à toi", ChordChart.convert("[Do]Gloire [Lam]à toi", doMajeur, Notation.LETTERS))
    }

    @Test
    fun `unknownChords repère les fautes de frappe`() {
        assertEquals(listOf("Dp", "N.C."), ChordChart.unknownChords("[Do][Dp][N.C.][5]", doMajeur))
    }

    @Test
    fun `render découpe les paroles sous les accords`() {
        val lines = ChordChart.render("Al[1]lé[5]luia", doMajeur, Notation.SOLFEGE)
        assertEquals(
            listOf(
                ChartLine.Lyrics(
                    listOf(
                        ChartSegment(null, "Al", continuesWord = true),
                        ChartSegment("Do", "lé", continuesWord = true),
                        ChartSegment("Sol", "luia"),
                    ),
                ),
            ),
            lines,
        )
    }

    @Test
    fun `render transpose toute la grille`() {
        val lines = ChordChart.render("[1][4][5]", Key(7), Notation.LETTERS)
        val chords = (lines.single() as ChartLine.Lyrics).segments.map { it.chord }
        assertEquals(listOf("G", "C", "D"), chords)
    }

    @Test
    fun `render reconnaît les titres de section et les lignes vides`() {
        val lines = ChordChart.render("# Refrain\n\n[1]Gloire", doMajeur, Notation.NASHVILLE)
        assertEquals(ChartLine.Section("Refrain"), lines[0])
        assertEquals(ChartLine.Lyrics(emptyList()), lines[1])
        assertEquals(ChartLine.Lyrics(listOf(ChartSegment("1", "Gloire"))), lines[2])
    }
}
