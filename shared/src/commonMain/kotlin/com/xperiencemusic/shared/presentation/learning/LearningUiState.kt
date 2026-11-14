package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.learning.ActivityGrader
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.variedActivity
import com.xperiencemusic.shared.domain.model.ActivityAnswer
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.LearningKind
import com.xperiencemusic.shared.domain.model.LearningModule
import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.canPostpone
import com.xperiencemusic.shared.domain.model.complete
import com.xperiencemusic.shared.domain.model.completedIn
import com.xperiencemusic.shared.domain.model.nextIn
import kotlinx.datetime.LocalDate

enum class LearningMode { HOME, ACTIVITY, SUMMARY }

/** What went wrong with the last action, said on the screen. */
enum class LearningError { SAVE_FAILED, REVIEW_NOT_SENT }

enum class MicrophoneIssue {
    /** The permission was refused: the screen offers the phone settings. */
    DENIED,

    /** Refused permission, a call using the microphone…: the take ended. */
    UNAVAILABLE,
}

/** The verdict shown under "Vérifier", with the help used to get there. */
data class CheckFeedback(val result: GradeResult, val hints: Int)

data class LearningUiState(
    val today: LocalDate = LocalDate(2026, 1, 1),
    val loading: Boolean = true,
    val allowed: Boolean = false,
    val memberId: String? = null,
    val module: LearningModule = LearningCatalog.module(LearningCatalog.DEFAULT),
    val index: Int = 0,
    val encounter: Int = 0,
    val progress: LearningProgress = LearningProgress(),
    val notes: Set<Int> = emptySet(),
    val answer: String? = null,
    val listened: Boolean = false,
    val result: Boolean? = null,
    val saving: Boolean = false,
    val error: LearningError? = null,
    val moduleReviews: Map<String, ExerciseStatus> = emptyMap(),
    val reviewLoaded: Boolean = false,
    val reviewStatus: ExerciseStatus? = null,
    /** The responsable's comment on the objective. */
    val reviewFeedback: String? = null,
    val reviewDue: Boolean = false,
    val mode: LearningMode = LearningMode.HOME,
    val hints: Int = 0,
    val showSteps: Boolean = false,
    val showSettings: Boolean = false,
    val preferences: LearningPreferences = LearningPreferences(),
    val checkFeedback: CheckFeedback? = null,
    val taps: List<Double> = emptyList(),
    val sessionDone: List<String> = emptyList(),
    val sessionDifficult: Set<String> = emptySet(),
    /** The title each worked activity had this session: a variant ("À toi · Si♭ majeur") differs from its base. */
    val sessionTitles: Map<String, String> = emptyMap(),
    val successSerial: Int = 0,
    /** Sung activities: the singer's comfort note, what the microphone hears and how far the take went. */
    val comfortMidi: Int = VoicePart.ALTO.comfortMidi,
    val singing: Boolean = false,
    val sung: Boolean = false,
    val heardMidi: Int? = null,
    val singStep: Int = 0,
    val holdSeconds: Double = 0.0,
    val micError: MicrophoneIssue? = null,
    /** While listening: distance to the note in cents, the input level, and how long nothing was heard. */
    val heardCents: Double? = null,
    val micLevel: Float = 0f,
    val silentSeconds: Double = 0.0,
    /** Sung activities set aside for later: never counted as done, they no longer lock the next steps. */
    val postponed: Set<String> = emptySet(),
    /** Where the member left off last time, if they ever opened an activity of this module. */
    val resumeIndex: Int? = null,
    /** The takes sent for this module's objective, newest first. */
    val takes: List<ObjectiveTakeUi> = emptyList(),
) {
    /** The furthest step open to the member. */
    val reached get() = progress.nextIn(module, postponed)

    /** The microphone was refused: the screen offers the phone settings. */
    val micDenied get() = micError == MicrophoneIssue.DENIED

    /** About three seconds without a voice: the screen says so instead of an empty "J’entends : …". */
    val hearsNothing get() = singing && silentSeconds >= SILENCE_WARNING_SECONDS

    /** "Reprendre" only stands for a real spot left behind, still open to the member. */
    val canResume get() = resumeIndex != null && resumeIndex in module.activities.indices && (resumeIndex <= reached || complete)

    val canPostpone get() = allowed && !saving && !singing && result != true && activity.canPostpone() && activity.id !in completed

    val singTargets get() = activity.sing?.semitones?.map { comfortMidi + it }.orEmpty()

    /** Ear questions without listed answers ask "Majeur ou mineur ?". */
    val choiceOptions get() = if (activity.kind == LearningKind.EAR && activity.choices.isEmpty()) listOf("Majeur", "Mineur") else activity.choices

    /** After a wrong chord: the selected notes that belong to it, shown next to the correction. */
    val keptNotes get() = if (result == false && activity.kind == LearningKind.KEYBOARD) notes.intersect(activity.expectedNotes) else null

    val activity get() = variedActivity(module.activities[index], encounter)

    val completed get() = progress.completedIn(module)

    val complete get() = progress.complete(module)

    val answerSoFar get() = ActivityAnswer(notes = notes, choice = answer, listened = listened, taps = taps, sung = sung)

    val canCheck get() = !saving && allowed && ActivityGrader.canSubmit(activity, answerSoFar)

    private companion object {
        const val SILENCE_WARNING_SECONDS = 3.0
    }
}

sealed interface LearningEvent {
    data class Open(val index: Int) : LearningEvent
    data class Key(val midi: Int) : LearningEvent
    data class Answer(val value: String) : LearningEvent
    data class Preferences(val value: LearningPreferences) : LearningEvent
    data object ChallengeExample : LearningEvent
    data object Begin : LearningEvent
    data object Resume : LearningEvent
    data object Home : LearningEvent
    data object Steps : LearningEvent
    data object Settings : LearningEvent
    data object Hint : LearningEvent
    data object Listen : LearningEvent
    data object ListenBroken : LearningEvent
    data object ListenSelection : LearningEvent
    data object Tap : LearningEvent
    data object ResetTaps : LearningEvent
    data object Metronome : LearningEvent
    data object StartSinging : LearningEvent
    data object StopSinging : LearningEvent
    data object MicrophoneDenied : LearningEvent
    data object Check : LearningEvent

    /** "Je le ferai plus tard": moves on from a sung activity without passing it. */
    data object Postpone : LearningEvent
    data object Next : LearningEvent
    data object RequestReview : LearningEvent
    data object Sync : LearningEvent

    /** Plays a take sent for the objective, or pauses it; the screen names it for the system controls. */
    data class PlayTake(val take: ObjectiveTakeUi, val title: String, val subtitle: String) : LearningEvent
}
