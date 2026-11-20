package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.model.servicesOf
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate

/** What waits for the member behind a tab: shown as a dot on it, whichever screen is open. */
data class TabAlerts(
    /** A service of a month still open that the member has not answered. */
    val planning: Boolean = false,
    /** A voice the reviewer sent back with a comment, not sung again yet. */
    val progress: Boolean = false,
)

class ObserveTabAlertsUseCase(
    private val observePlanning: ObservePlanningUseCase,
    private val observeCurrentMember: ObserveCurrentMemberUseCase,
    private val voices: VoiceSubmissionRepository,
    private val getToday: GetTodayUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<TabAlerts> {
        // Only singers send their voice: nothing is fetched for the others.
        val myVoices = observeCurrentMember()
            .distinctUntilChanged { old, new -> old?.id == new?.id && old?.isSinger == new?.isSinger }
            .flatMapLatest { member ->
                if (member?.isSinger == true) voices.observeMine(member.id).onStart { emit(emptyList()) } else flowOf(emptyList())
            }
            // A dot is a hint: without the reviews, the tab simply shows none.
            .catch { emit(emptyList()) }
        return combine(observePlanning(), myVoices) { planning, mine ->
            TabAlerts(planning = planning.hasUnansweredService(getToday()), progress = mine.hasVoiceToRework())
        }.distinctUntilChanged()
    }
}

private val Member.isSinger: Boolean get() = profile.instrument == Instrument.SINGER

/** Counts like the planning screen: the services still to come of the months open for answers. */
fun Planning.hasUnansweredService(today: LocalDate): Boolean {
    val myId = me?.id ?: return false
    val answered = signups.filter { it.memberId == myId }.map { it.serviceDate }.toSet()
    return months.filterNot { it.isValidated }.any { month ->
        servicesOf(month.monthStart).any { it >= today && it !in answered }
    }
}

fun List<VoiceSubmission>.hasVoiceToRework(): Boolean = any { it.status == ExerciseStatus.TO_REWORK }
