package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SongLink
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceTrack
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Demo content until the band's library lives in Supabase. Only public-domain hymns are used, so
 * no copyrighted lyrics ship with the app.
 */
internal object SampleSongs {

    fun all(today: LocalDate): List<Song> = listOf(
        Song(
            id = "a-toi-la-gloire",
            title = "À toi la gloire",
            artist = "E. Budry · G. F. Haendel",
            key = Key(2), // Ré
            chart = """
                # Couplet
                [1]À toi la [5]gloire, [1]Ô Ressus[5]ci[1]té !
                À toi la [5]vic[1]toire [4]pour l'éter[5]ni[1]té !
                # Refrain
                [1]À toi la [5]gloire, [1]Ô Ressus[5]ci[1]té !
            """.trimIndent(),
            recordings = listOf(
                recording("atlg-1", "Répétition du mardi", today.minus(DatePeriod(months = 5)), 212, isReference = true),
                recording("atlg-2", "Version acoustique", today.minus(DatePeriod(months = 2)), 198),
            ),
            voiceTracks = listOf(
                VoiceTrack("atlg-s", VoicePart.SOPRANO, "demo://a-toi-la-gloire/soprano"),
                VoiceTrack("atlg-a", VoicePart.ALTO, "demo://a-toi-la-gloire/alto"),
                VoiceTrack("atlg-t", VoicePart.TENOR, "demo://a-toi-la-gloire/tenor"),
            ),
            links = emptyList(),
        ),
        Song(
            id = "amazing-grace",
            title = "Amazing Grace",
            artist = "John Newton",
            key = Key(7), // Sol
            chart = """
                # Couplet
                A[1]mazing [1/3]grace, how [4]sweet the [1]sound
                That [1]saved a wretch like [5]me
                I [1]once was lost, but [4]now am [1]found
                Was [6m]blind but [5]now I [1]see
            """.trimIndent(),
            recordings = listOf(
                recording("ag-1", "Prestation du dimanche", today.minus(DatePeriod(months = 10)), 245, isReference = true),
            ),
            voiceTracks = emptyList(),
            links = listOf(SongLink("Partition (IMSLP)", "https://imslp.org/wiki/Amazing_Grace")),
        ),
        Song(
            id = "holy-holy-holy",
            title = "Holy, Holy, Holy",
            artist = "Reginald Heber · John B. Dykes",
            key = Key(2), // Ré
            chart = """
                # Couplet
                [1]Holy, holy, [6m]holy! [5]Lord God Al[1]mighty!
                [1]Early in the [4]morning our [1]song shall [5]rise to thee
                [1]Holy, holy, [6m]holy! [4]Merciful and [5]mighty!
                [6m]God in three [4]persons, [1/5]blessed [5]Trini[1]ty!
            """.trimIndent(),
            recordings = emptyList(),
            voiceTracks = emptyList(),
            links = emptyList(),
        ),
        Song(
            id = "exemple-grille",
            title = "Grille d'exemple (4 accords)",
            artist = null,
            key = Key(9), // La
            chart = """
                # Intro
                [1] [5] [6m] [4]
                # Pont
                [4]Ligne d'exemple sur [5]quatre ac[6m]cords
                [4]Pour essayer la [5]transposi[1]tion
            """.trimIndent(),
            recordings = emptyList(),
            voiceTracks = emptyList(),
            links = emptyList(),
        ),
    )

    private fun recording(
        id: String,
        title: String,
        recordedOn: LocalDate,
        durationSeconds: Int,
        isReference: Boolean = false,
    ) = Recording(id, title, recordedOn, durationSeconds, "demo://$id", isReference)
}
