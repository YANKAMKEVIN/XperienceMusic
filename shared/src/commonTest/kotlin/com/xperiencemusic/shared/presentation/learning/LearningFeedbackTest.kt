package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.LearningKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LearningFeedbackTest {
    private fun stateOn(kind: LearningKind, withChoices: Boolean? = null): LearningUiState {
        val module = LearningCatalog.modules.first { m ->
            m.activities.any { it.kind == kind && (withChoices == null || it.choices.isNotEmpty() == withChoices) }
        }
        val index = module.activities.indexOfFirst { it.kind == kind && (withChoices == null || it.choices.isNotEmpty() == withChoices) }
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
}
