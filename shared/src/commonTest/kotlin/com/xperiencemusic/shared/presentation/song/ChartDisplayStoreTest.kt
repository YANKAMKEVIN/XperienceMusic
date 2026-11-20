package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.data.local.FileDevicePreferences
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChartDisplayStoreTest {
    @Test
    fun `sans choix, les accords sont en chiffres`() {
        assertEquals(Notation.NASHVILLE, ChartDisplayStore(FileDevicePreferences(FakeFileStore())).notation.value)
    }

    @Test
    fun `la notation choisie est retrouvée à la réouverture de l'app`() {
        val files = FakeFileStore()
        ChartDisplayStore(FileDevicePreferences(files)).selectNotation(Notation.SOLFEGE)
        assertEquals(Notation.SOLFEGE, ChartDisplayStore(FileDevicePreferences(files)).notation.value)
    }

    @Test
    fun `une valeur illisible revient aux chiffres`() {
        val files = FakeFileStore()
        FileDevicePreferences(files).write("chart.notation", "TABLATURE")
        assertEquals(Notation.NASHVILLE, ChartDisplayStore(FileDevicePreferences(files)).notation.value)
    }

    @Test
    fun `paroles seules reste indécis tant que le membre n'a pas choisi, puis est retenu`() {
        val files = FakeFileStore()
        val store = ChartDisplayStore(FileDevicePreferences(files))
        assertNull(store.lyricsOnly.value)
        store.setLyricsOnly(false)
        assertEquals(false, ChartDisplayStore(FileDevicePreferences(files)).lyricsOnly.value)
    }
}
