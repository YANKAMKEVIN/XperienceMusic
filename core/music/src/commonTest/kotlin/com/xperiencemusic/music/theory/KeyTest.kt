package com.xperiencemusic.music.theory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyTest {

    @Test
    fun `parse lit les tonalités en solfège et en lettres`() {
        assertEquals(Key(0), Key.parse("Do"))
        assertEquals(Key(7), Key.parse("G"))
        assertEquals(Key(10), Key.parse("Sib"))
        assertEquals(Key(2, Mode.MINOR), Key.parse("Rém"))
        assertEquals(Key(9, Mode.MINOR), Key.parse("Am"))
        assertEquals(Key(4, Mode.MINOR), Key.parse("Mi mineur"))
    }

    @Test
    fun `parse refuse un texte qui n'est pas une tonalité`() {
        assertNull(Key.parse("Bonjour"))
        assertNull(Key.parse("Cmaj7"))
    }

    @Test
    fun `les tonalités bémolisées utilisent des bémols`() {
        assertTrue(Key(5).usesFlats) // Fa
        assertTrue(Key(2, Mode.MINOR).usesFlats) // Ré mineur, relatif de Fa
        assertFalse(Key(7).usesFlats) // Sol
        assertTrue(Key(3, Mode.MINOR).usesFlats) // Mib mineur, plutôt que Ré# mineur
        assertFalse(Key(1, Mode.MINOR).usesFlats) // Do# mineur
        assertFalse(Key(8, Mode.MINOR).usesFlats) // Sol# mineur
    }

    @Test
    fun `degreeOf écrit les notes hors gamme en degrés bémolisés`() {
        val doMajeur = Key(0)
        assertEquals(ScaleDegree(5), doMajeur.degreeOf(7))
        assertEquals(ScaleDegree(7, -1), doMajeur.degreeOf(10)) // Sib -> b7
        assertEquals(ScaleDegree(3, -1), doMajeur.degreeOf(3)) // Mib -> b3
        assertEquals(ScaleDegree(4, 1), doMajeur.degreeOf(6)) // Fa# -> #4
    }

    @Test
    fun `transposedBy fait le tour de l'octave`() {
        assertEquals(Key(1), Key(10).transposedBy(3))
        assertEquals(Key(11), Key(0).transposedBy(-1))
    }

    @Test
    fun `label affiche la tonalité dans la notation choisie`() {
        assertEquals("Sib majeur", Key(10).label(Notation.SOLFEGE))
        assertEquals("Bb", Key(10).label(Notation.LETTERS))
        assertEquals("Em", Key(4, Mode.MINOR).label(Notation.LETTERS))
    }
}
