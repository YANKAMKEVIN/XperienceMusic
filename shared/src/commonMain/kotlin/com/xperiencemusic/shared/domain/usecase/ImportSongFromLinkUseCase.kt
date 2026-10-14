package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.chart.ChordProImport
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.SongPageSource

/** A song read from a page, ready to fill the editor; nothing is saved yet. */
data class ImportedSong(
    val title: String,
    val artist: String?,
    val key: Key?,
    /** The chart with a closing "Source" section: the credits and the page the song came from. */
    val chart: String,
)

/**
 * Fills a new song from one accords.app page the admin picked, e.g. `accords.app/JEM414`. The
 * credits of the page (copyright, collection) and its address are kept at the end of the chart.
 */
class ImportSongFromLinkUseCase(private val pages: SongPageSource) {
    suspend operator fun invoke(link: String): DomainResult<ImportedSong> {
        val pageUrl = accordsPageUrl(link) ?: return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote {
            val chordPro = pages.chordPro(pageUrl) ?: throw DomainException(DomainError.NOT_FOUND)
            val song = ChordProImport.parse(chordPro)
            val title = song.title?.trim()?.takeIf { it.isNotEmpty() } ?: throw DomainException(DomainError.NOT_FOUND)
            val source = listOf("# Source") + song.credits + pageUrl.removePrefix("https://")
            ImportedSong(
                title = title,
                artist = song.artist,
                key = song.key,
                chart = (listOf(song.chart) + source).filter { it.isNotEmpty() }.joinToString("\n"),
            )
        }
    }
}

private val songPath = Regex("""^/[A-Za-z]*\d+[A-Za-z0-9-]*$""")

/**
 * `https://accords.app/<song>` for a link to one song of accords.app, with or without scheme, `www.`
 * or query; null for anything else, so the app never reads other sites or the catalogue pages.
 */
internal fun accordsPageUrl(link: String): String? {
    val trimmed = link.trim().substringBefore('#').substringBefore('?').trimEnd('/')
    val withoutScheme = trimmed.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    if (!withoutScheme.startsWith("accords.app/")) return null
    val path = withoutScheme.removePrefix("accords.app")
    return if (songPath.matches(path)) "https://accords.app$path" else null
}
