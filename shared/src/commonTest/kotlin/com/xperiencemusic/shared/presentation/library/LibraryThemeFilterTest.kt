package com.xperiencemusic.shared.presentation.library

import com.xperiencemusic.shared.domain.model.Repertoire
import com.xperiencemusic.shared.domain.usecase.SongSearchIndex
import com.xperiencemusic.shared.fakes.song
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The theme filter: hidden while no song has a theme, then narrowing the tabs and the search. */
class LibraryThemeFilterTest {
    private val grace = song("1", "Amazing Grace", chart = "[1]Grace").copy(themes = listOf("louange"))
    private val digne = song("2", "Tu es digne", chart = "[1]Gloire").copy(themes = listOf("adoration", "louange"))
    private val noel = song("3", "Il est né", chart = "[1]Gloire").copy(themes = listOf("Évangile", "noël"))
    private val plain = song("4", "Béni soit ton nom", chart = "[1]Gloire")
    private val index = SongSearchIndex(listOf(grace, digne, noel, plain))

    private fun LibraryUiState.ids() = sections.flatMap { it.songs }.map { it.id }

    @Test
    fun `sans thème importé, le filtre est caché et rien ne change`() {
        val state = libraryContent(SongSearchIndex(listOf(plain)), Repertoire.EMPTY, "", null, chosenTheme = "louange")
        assertTrue(state.themes.isEmpty())
        assertNull(state.selectedTheme)
        assertEquals(listOf("4"), state.ids())
    }

    @Test
    fun `les thèmes sont proposés de A à Z, accents compris`() {
        val state = libraryContent(index, Repertoire.EMPTY, "", null)
        assertEquals(listOf("adoration", "Évangile", "louange", "noël"), state.themes)
    }

    @Test
    fun `un thème restreint le catalogue et ses compteurs`() {
        val state = libraryContent(index, Repertoire.EMPTY, "", LibraryTab.CATALOGUE, chosenTheme = "louange")
        assertEquals("louange", state.selectedTheme)
        assertEquals(listOf("1", "2"), state.ids())
        assertEquals(2, state.catalogueCount)
    }

    @Test
    fun `un thème restreint aussi la recherche, paroles comprises`() {
        val state = libraryContent(index, Repertoire.EMPTY, "gloire", null, chosenTheme = "noël")
        assertEquals(listOf("3"), state.ids())
    }

    @Test
    fun `un thème restreint le répertoire, prochain service compris`() {
        val repertoire = Repertoire(LocalDate(2026, 11, 22), listOf(digne, grace), emptyMap(), listOf(digne, grace, plain))
        val state = libraryContent(index, repertoire, "", LibraryTab.REPERTOIRE, chosenTheme = "adoration")
        assertEquals(listOf("2"), state.ids())
        assertEquals(1, state.repertoireCount)
    }

    @Test
    fun `un thème disparu du catalogue n'est plus appliqué`() {
        val state = libraryContent(index, Repertoire.EMPTY, "", LibraryTab.CATALOGUE, chosenTheme = "pâques")
        assertNull(state.selectedTheme)
        assertEquals(4, state.catalogueCount)
    }
}
