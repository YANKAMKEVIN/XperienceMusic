package com.xperiencemusic.music.theory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChordTest {
    private val doMajeur = Key(0)
    private val solMajeur = Key(7)

    @Test
    fun `parse lit les chiffres Nashville`() {
        assertEquals(Chord(ScaleDegree(6), "m"), Chord.parse("6m", doMajeur))
        assertEquals(Chord(ScaleDegree(7, -1)), Chord.parse("b7", doMajeur))
        assertEquals(Chord(ScaleDegree(4), bass = ScaleDegree(1)), Chord.parse("4/1", doMajeur))
    }

    @Test
    fun `parse convertit le solfège et les lettres en degrés`() {
        assertEquals(Chord(ScaleDegree(2), "m7"), Chord.parse("Rém7", doMajeur))
        assertEquals(Chord(ScaleDegree(5), bass = ScaleDegree(7)), Chord.parse("Sol/Si", doMajeur))
        assertEquals(Chord(ScaleDegree(1), "add9"), Chord.parse("Gadd9", solMajeur))
        assertEquals(Chord(ScaleDegree(4), "add9"), Chord.parse("Fadd9", doMajeur))
        assertEquals(Chord(ScaleDegree(6), "m"), Chord.parse("Em", solMajeur))
    }

    @Test
    fun `parse ignore ce qui n'est pas un accord`() {
        assertNull(Chord.parse("N.C.", doMajeur))
        assertNull(Chord.parse("x2", doMajeur))
        assertNull(Chord.parse("", doMajeur))
    }

    @Test
    fun `render affiche l'accord dans chaque notation`() {
        val chord = Chord(ScaleDegree(6), "m7", bass = ScaleDegree(5))
        assertEquals("6m7/5", chord.render(doMajeur, Notation.NASHVILLE))
        assertEquals("Lam7/Sol", chord.render(doMajeur, Notation.SOLFEGE))
        assertEquals("Am7/G", chord.render(doMajeur, Notation.LETTERS))
    }

    @Test
    fun `render transpose en changeant simplement de tonalité`() {
        val chord = Chord(ScaleDegree(4))
        assertEquals("Fa", chord.render(doMajeur, Notation.SOLFEGE))
        assertEquals("Do", chord.render(solMajeur, Notation.SOLFEGE))
        assertEquals("Eb", chord.render(Key(10), Notation.LETTERS)) // Sib majeur
    }
}
