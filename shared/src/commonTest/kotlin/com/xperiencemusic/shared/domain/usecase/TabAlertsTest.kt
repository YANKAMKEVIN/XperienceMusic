package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.model.servicesOf
import com.xperiencemusic.shared.fakes.member
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabAlertsTest {
    private val november = LocalDate(2026, 11, 1)
    private val today = LocalDate(2026, 11, 19)
    private val me = member()

    private fun planning(months: List<PlanningMonth>, answered: List<LocalDate> = emptyList(), signedIn: Boolean = true) = Planning(
        months = months,
        signups = answered.map { ServiceSignup(it, me.id, serves = true) } +
            // Someone else's answers never count for me.
            servicesOf(november).map { ServiceSignup(it, "other", serves = true) },
        members = emptyList(),
        me = me.takeIf { signedIn },
    )

    @Test
    fun `un service à venir sans réponse dans un mois ouvert allume le Planning`() {
        val open = listOf(PlanningMonth(november, answerBy = null, isValidated = false))
        assertTrue(planning(open).hasUnansweredService(today))
        // Past services cannot be answered any more: only the coming ones are waited for.
        val coming = servicesOf(november).filter { it >= today }
        assertTrue(planning(open, answered = coming.drop(1)).hasUnansweredService(today))
        assertFalse(planning(open, answered = coming).hasUnansweredService(today))
    }

    @Test
    fun `un mois validé, pas ouvert ou sans membre connecté n'attend rien`() {
        assertFalse(planning(listOf(PlanningMonth(november, null, isValidated = true))).hasUnansweredService(today))
        assertFalse(planning(emptyList()).hasUnansweredService(today))
        assertFalse(planning(listOf(PlanningMonth(november, null, isValidated = false)), signedIn = false).hasUnansweredService(today))
    }

    @Test
    fun `une voix renvoyée par le responsable allume Progrès jusqu'à ce qu'elle soit rechantée`() {
        fun voice(status: ExerciseStatus) =
            VoiceSubmission("v", me.id, "Grâce", null, "s1", "Chant", "voices/v.m4a", 30, today, status, feedback = "Plus de souffle")
        assertTrue(listOf(voice(ExerciseStatus.VALIDATED), voice(ExerciseStatus.TO_REWORK)).hasVoiceToRework())
        assertFalse(listOf(voice(ExerciseStatus.SUBMITTED), voice(ExerciseStatus.VALIDATED)).hasVoiceToRework())
        assertFalse(emptyList<VoiceSubmission>().hasVoiceToRework())
    }
}
