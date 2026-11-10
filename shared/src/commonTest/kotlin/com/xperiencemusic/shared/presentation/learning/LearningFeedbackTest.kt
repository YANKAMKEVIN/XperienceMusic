package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.LearningKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningFeedbackTest {
    private fun stateOn(kind: LearningKind, withChoices: Boolean? = null): LearningUiState {
        fun matches(activity: com.xperiencemusic.shared.domain.model.LearningActivity) =
            activity.kind == kind && (withChoices == null || activity.choices.isNotEmpty() == withChoices)
        val module = LearningCatalog.modules.first { m -> m.activities.any(::matches) }
        val index = module.activities.indexOfFirst(::matches)
        return LearningUiState(loading = false, allowed = true, module = module, index = index, mode = LearningMode.ACTIVITY)
    }

    @Test
    fun `après un accord faux, la correction garde les bonnes notes choisies`() {
        val keyboard = stateOn(LearningKind.KEYBOARD)
        val expected = keyboard.activity.expectedNotes
        val right = expected.first()
        val wrong = (0..127).first { it !in expected }
        val checked = keyboard.copy(notes = setOf(right, wrong), result = false)
        assertEquals(setOf(right), checked.keptNotes)
        assertNull(checked.copy(result = true).keptNotes, "nothing to keep once it is right")
        assertNull(keyboard.copy(notes = setOf(right, wrong)).keptNotes, "nothing before checking")
    }

    @Test
    fun `une question d'oreille sans réponses listées propose majeur ou mineur`() {
        assertEquals(listOf("Majeur", "Mineur"), stateOn(LearningKind.EAR, withChoices = false).choiceOptions)
        val choice = stateOn(LearningKind.CHOICE, withChoices = true)
        assertEquals(choice.activity.choices, choice.choiceOptions)
    }

    @Test
    fun `vérifier attend une réponse, et jamais pendant une sauvegarde`() {
        val keyboard = stateOn(LearningKind.KEYBOARD)
        assertFalse(keyboard.canCheck)
        assertTrue(keyboard.copy(notes = setOf(60)).canCheck)
        assertFalse(keyboard.copy(notes = setOf(60), saving = true).canCheck)
        assertFalse(keyboard.copy(notes = setOf(60), allowed = false).canCheck)
    }
}
