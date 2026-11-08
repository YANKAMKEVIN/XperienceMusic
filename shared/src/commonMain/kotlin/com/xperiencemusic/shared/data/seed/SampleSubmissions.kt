package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Demo mode: Grâce, the demo band's alto, has already sent two parts, so the review screen has
 * something to hear. Daniel plays the piano: he has no voice to send.
 */
internal object SampleSubmissions {
    fun around(today: LocalDate) = listOf(
        VoiceSubmission(
            "sample-1", "demo-grace", "Grâce", VoicePart.ALTO, "a-toi-la-gloire", "À toi la gloire",
            "demo/submissions/grace.wav", 184, today.minus(DatePeriod(days = 1)), ExerciseStatus.SUBMITTED, null,
        ),
        VoiceSubmission(
            "sample-2", "demo-grace", "Grâce", VoicePart.ALTO, "amazing-grace", "Amazing Grace",
            "demo/submissions/daniel.wav", 201, today.minus(DatePeriod(days = 3)), ExerciseStatus.SUBMITTED, null,
        ),
    )
}
