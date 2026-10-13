package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Demo mode: two singers have already sent their part, so the review screen has something to hear. */
internal object SampleSubmissions {
    fun around(today: LocalDate) = listOf(
        VoiceSubmission(
            "sample-1", "demo-grace", "Grâce", VoicePart.SOPRANO, "a-toi-la-gloire", "À toi la gloire",
            "demo/submissions/grace.wav", 184, today.minus(DatePeriod(days = 1)), ExerciseStatus.SUBMITTED, null,
        ),
        VoiceSubmission(
            "sample-2", "demo-daniel", "Daniel", VoicePart.TENOR, "amazing-grace", "Amazing Grace",
            "demo/submissions/daniel.wav", 201, today.minus(DatePeriod(days = 3)), ExerciseStatus.SUBMITTED, null,
        ),
    )
}
