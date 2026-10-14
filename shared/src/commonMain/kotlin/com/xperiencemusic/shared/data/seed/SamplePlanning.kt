package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.servicesOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Demo planning: this month validated, next month open for sign-ups. */
object SamplePlanning {
    fun months(today: LocalDate): List<PlanningMonth> {
        val thisMonth = today.monthStart()
        val nextMonth = thisMonth.plus(DatePeriod(months = 1))
        return listOf(
            PlanningMonth(thisMonth, answerBy = null, isValidated = true),
            PlanningMonth(nextMonth, answerBy = nextMonth.minus(DatePeriod(days = 7)), isValidated = false),
        )
    }

    /** Grâce serves every other service, Daniel on every Sunday; the person trying the demo answers. */
    fun signups(today: LocalDate): List<ServiceSignup> {
        val thisMonth = today.monthStart()
        val nextMonth = thisMonth.plus(DatePeriod(months = 1))
        return (servicesOf(thisMonth) + servicesOf(nextMonth)).flatMapIndexed { index, date ->
            listOfNotNull(
                ServiceSignup(date, "demo-grace", serves = index % 2 == 0),
                ServiceSignup(date, "demo-daniel", serves = true).takeIf { date.dayOfWeek.name == "SUNDAY" },
            )
        }
    }
}
