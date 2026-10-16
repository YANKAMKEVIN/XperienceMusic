package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Demo mode: Grâce asks to become Confirmée and Daniel ticked two piano objectives. */
internal object SampleGrowth {
    fun requests(today: LocalDate) = listOf(
        LevelRequest(
            "sample-level-1", "demo-grace", "Grâce", Instrument.SINGER, VoicePart.ALTO, Level.CONFIRMED,
            ExerciseStatus.SUBMITTED, null, today.minus(DatePeriod(days = 2)),
        ),
    )

    fun checks(today: LocalDate) = listOf(
        ObjectiveCheck("demo-daniel", "Daniel", Instrument.PIANO, "piano-c1", ExerciseStatus.SUBMITTED, null, today.minus(DatePeriod(days = 1))),
        ObjectiveCheck("demo-daniel", "Daniel", Instrument.PIANO, "piano-c2", ExerciseStatus.VALIDATED, null, today.minus(DatePeriod(days = 8))),
    )
}
