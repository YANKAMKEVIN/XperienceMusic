package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The band serves on Fridays (no rehearsal, no setlist) and on Sundays (after Tuesday's rehearsal). */
enum class ServiceKind { FRIDAY, SUNDAY }

fun LocalDate.serviceKind(): ServiceKind? = when (dayOfWeek) {
    DayOfWeek.FRIDAY -> ServiceKind.FRIDAY
    DayOfWeek.SUNDAY -> ServiceKind.SUNDAY
    else -> null
}

/** First day of the month [this] falls in. */
fun LocalDate.monthStart(): LocalDate = LocalDate(year, month, 1)

/** Every Friday and Sunday of the month starting on [monthStart], in order. */
fun servicesOf(monthStart: LocalDate): List<LocalDate> =
    generateSequence(monthStart) { it.plus(DatePeriod(days = 1)) }
        .takeWhile { it.month == monthStart.month }
        .filter { it.serviceKind() != null }
        .toList()

/** A month an admin opened for sign-ups. Until it is validated, members can change their answers. */
data class PlanningMonth(
    val monthStart: LocalDate,
    /** Members who have not answered are reminded until this date. */
    val answerBy: LocalDate?,
    val isValidated: Boolean,
)

/** One member's answer for one service. */
data class ServiceSignup(
    val serviceDate: LocalDate,
    val memberId: String,
    val serves: Boolean,
)

/** Who serves on [date], and which parts nobody covers yet. */
data class ServiceTeam(
    val date: LocalDate,
    val kind: ServiceKind,
    val serving: List<BandMember>,
    val missing: List<Instrument>,
)

/** The band's parts every service needs at least one of. */
val requiredParts: List<Instrument> = listOf(Instrument.SINGER, Instrument.PIANO, Instrument.BASS, Instrument.DRUMS)

/**
 * The team of each service in the month: the members who answered "je sers", in band order (singers
 * by voice, then piano, bass, drums), and the parts left uncovered.
 */
fun planTeams(monthStart: LocalDate, members: List<BandMember>, signups: List<ServiceSignup>): List<ServiceTeam> {
    val byId = members.associateBy { it.id }
    return servicesOf(monthStart).map { date ->
        val serving = signups
            .filter { it.serviceDate == date && it.serves }
            .mapNotNull { byId[it.memberId] }
            .sortedWith(compareBy({ it.instrument?.ordinal ?: Int.MAX_VALUE }, { it.voicePart?.ordinal ?: 0 }, { it.displayName.orEmpty() }))
        ServiceTeam(
            date = date,
            kind = date.serviceKind()!!,
            serving = serving,
            missing = requiredParts.filter { part -> serving.none { it.instrument == part } },
        )
    }
}
