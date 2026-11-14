package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.model.LearningModule
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.dueIn
import com.xperiencemusic.shared.domain.model.nextIn
import kotlinx.datetime.LocalDate

/** What the member is doing on the screen, before it is combined with their data. */
internal data class LearningForm(
    val index: Int? = null,
    val encounter: Int = 0,
    val notes: Set<Int> = emptySet(),
    val answer: String? = null,
    val listened: Boolean = false,
    val result: Boolean? = null,
    val saving: Boolean = false,
    val error: LearningError? = null,
    val hints: Int = 0,
    val mode: LearningMode = LearningMode.HOME,
    val steps: Boolean = false,
    val settings: Boolean = false,
    val preferences: LearningPreferences = LearningPreferences(),
    val feedback: CheckFeedback? = null,
    val taps: List<Double> = emptyList(),
    val plan: List<Int> = emptyList(),
    val done: List<String> = emptyList(),
    val difficult: Set<String> = emptySet(),
    val titles: Map<String, String> = emptyMap(),
    val successSerial: Int = 0,
    val singing: Boolean = false,
    val sung: Boolean = false,
    val heardMidi: Int? = null,
    val singStep: Int = 0,
    val holdSeconds: Double = 0.0,
    val micError: MicrophoneIssue? = null,
    val heardCents: Double? = null,
    val micLevel: Float = 0f,
    val silentSeconds: Double = 0.0,
    val postponed: Set<String> = emptySet(),
    val resumeIndex: Int? = null,
)

/** The member's data around the module, as observed. */
internal data class LearningData(
    val member: Member?,
    val progress: LearningProgress,
    /** The member's objective checks; null until the server answered. */
    val checks: List<ObjectiveCheck>?,
    val takes: List<ObjectiveTakeUi>,
)

internal object LearningUiStateMapper {
    fun map(module: LearningModule, today: LocalDate, data: LearningData, form: LearningForm): LearningUiState {
        val member = data.member
        val progress = data.progress
        val check = data.checks?.firstOrNull { it.objectiveId == module.objectiveId }
        return LearningUiState(
            today = today,
            loading = member == null,
            allowed = member?.profile?.instrument == module.instrument,
            memberId = member?.id,
            module = module,
            index = form.index ?: progress.nextIn(module, form.postponed),
            encounter = form.encounter,
            progress = progress,
            notes = form.notes,
            answer = form.answer,
            listened = form.listened,
            result = form.result,
            saving = form.saving,
            error = form.error,
            moduleReviews = data.checks.orEmpty().associate { it.objectiveId to it.status },
            reviewLoaded = data.checks != null,
            reviewStatus = check?.status,
            reviewFeedback = check?.feedback,
            reviewDue = progress.dueIn(module, today).isNotEmpty(),
            mode = form.mode,
            hints = form.hints,
            showSteps = form.steps,
            showSettings = form.settings,
            preferences = form.preferences,
            checkFeedback = form.feedback,
            taps = form.taps,
            sessionDone = form.done,
            sessionDifficult = form.difficult,
            sessionTitles = form.titles,
            successSerial = form.successSerial,
            comfortMidi = (member?.profile?.voicePart ?: VoicePart.ALTO).comfortMidi,
            singing = form.singing,
            sung = form.sung,
            heardMidi = form.heardMidi,
            singStep = form.singStep,
            holdSeconds = form.holdSeconds,
            micError = form.micError,
            heardCents = form.heardCents,
            micLevel = form.micLevel,
            silentSeconds = form.silentSeconds,
            postponed = form.postponed,
            resumeIndex = form.resumeIndex,
            takes = data.takes,
        )
    }
}
