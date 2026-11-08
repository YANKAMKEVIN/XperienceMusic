package com.xperiencemusic.shared.presentation.setlisteditor

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetlistDateWarningTest {
    private val today = LocalDate(2026, 11, 8)
    private fun warning(rehearsal: LocalDate, service: LocalDate, original: LocalDate? = null) = deriveSetlistEditorState(
        SetlistDraft(rehearsal, service, today = today, original = original?.let { SetlistChoices(it, service, "", emptyList()) }),
        emptyList(), emptyList(),
    ).dateWarning

    @Test
    fun `une semaine habituelle ne déclenche aucun avertissement`() {
        assertNull(warning(LocalDate(2026, 11, 10), LocalDate(2026, 11, 15)))
    }

    @Test
    fun `une répétition choisie dans le passé est signalée`() {
        assertEquals(SetlistDateWarning.PAST_REHEARSAL, warning(LocalDate(2026, 11, 1), LocalDate(2026, 11, 22)))
    }

    @Test
    fun `une setlist déjà publiée dans le passé n'est pas signalée`() {
        val rehearsal = LocalDate(2026, 11, 3)
        assertNull(warning(rehearsal, LocalDate(2026, 11, 8), original = rehearsal))
    }

    @Test
    fun `plus de trois semaines avant la prestation est signalé`() {
        assertEquals(SetlistDateWarning.LONG_GAP, warning(LocalDate(2026, 11, 10), LocalDate(2026, 12, 6)))
    }

    @Test
    fun `un conflit de dates reste affiché seul`() {
        assertNull(warning(LocalDate(2026, 11, 1), LocalDate(2026, 10, 25)))
    }
}
