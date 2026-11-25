package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Searches the catalogue, ignoring case and accents. */
class SearchSongsUseCase(private val songs: SongRepository) {
    /** Songs whose title or artist holds [query], best matches first; every song, by title, when it is blank. */
    operator fun invoke(query: String): Flow<List<Song>> =
        index().map { index -> index.search(query).map { it.song } }

    /** The catalogue ready to be searched many times, lyrics included: build it once, query it on each keystroke. */
    fun index(): Flow<SongSearchIndex> = songs.observeSongs().map(::SongSearchIndex)

    /**
     * The list of songs comes without the lyrics. The first search long enough to read them (see
     * [SongSearchIndex.readsLyrics]) calls this, which fetches every chart once per app session:
     * the matches in the titles show at once, those in the lyrics as soon as the charts arrive and
     * [index] emits again.
     *
     * Why not a search on the server (`ilike` or full text)? The charts hold the chords inside the
     * words (`Sei[2]gneur`) and the accents as typed: only the phone strips the chords and folds
     * the accents the way the search always has. It also keeps working offline from the copy on
     * the phone, and needs no change to the database.
     */
    suspend fun loadLyrics(): DomainResult<Unit> = runRemote { songs.loadLyrics() }
}

/**
 * Asks [SearchSongsUseCase.loadLyrics] once, on the first query that reads the lyrics; again on a
 * later query when it failed (no network).
 */
class LyricsOnDemand(private val searchSongs: SearchSongsUseCase) {
    private var state = State.NOT_ASKED

    /**
     * True when [query] needs the lyrics and they were not asked for yet: the caller then calls
     * [load]. The next queries answer false meanwhile, so one fetch runs at a time.
     */
    fun shouldLoad(query: String): Boolean {
        if (state == State.ASKED || !SongSearchIndex.readsLyrics(query)) return false
        state = State.ASKED
        return true
    }

    suspend fun load() {
        if (searchSongs.loadLyrics() is DomainResult.Failure) state = State.NOT_ASKED
    }

    private enum class State { NOT_ASKED, ASKED }
}

/** A song found by a search; [lyricLine] is the line that matched when neither the title nor the artist did. */
data class SongMatch(val song: Song, val lyricLine: String? = null)

/**
 * The songs with their text folded for searching. Lyrics are only folded the first time a search
 * needs them: with a couple of thousand songs that is the expensive part.
 */
class SongSearchIndex(songs: List<Song>) {
    private class Entry(val song: Song) {
        val title = song.title.foldForSearch()
        val artist = song.artist.orEmpty().foldForSearch()
        val lyrics by lazy { song.chart.orEmpty().lyricLines().joinToString("\n") { it.foldForSearch() } }
    }

    private val entries = songs.map(::Entry).sortedBy { it.title.trim() }

    /** Every song, sorted by title. */
    val songs: List<Song> = entries.map { it.song }

    /**
     * Titles starting with [query] first, then titles holding it, then artists, then, with
     * [inLyrics], lyrics; each group by title. After them, the songs that hold every word of
     * [query] apart or in another order ("peur esclave"): what people remember of a song is rarely
     * its exact wording.
     */
    fun search(query: String, inLyrics: Boolean = false): List<SongMatch> {
        val needle = query.trim().foldForSearch()
        if (needle.isEmpty()) return songs.map { SongMatch(it) }
        val searchLyrics = inLyrics && needle.length >= MIN_LYRICS_QUERY
        val words = needle.split(' ').filter { it.isNotEmpty() }.distinct()
        val ranked = entries.mapNotNull { entry ->
            when {
                entry.title.startsWith(needle) -> 0 to SongMatch(entry.song)
                needle in entry.title -> 1 to SongMatch(entry.song)
                needle in entry.artist -> 2 to SongMatch(entry.song)
                searchLyrics && needle in entry.lyrics -> 3 to SongMatch(entry.song, entry.song.lyricLine(needle))
                words.size < 2 -> null
                words.all { it in entry.title || it in entry.artist } -> 4 to SongMatch(entry.song)
                searchLyrics && words.all { it in entry.lyrics } -> 5 to SongMatch(entry.song, entry.song.lyricLine(words))
                else -> null
            }
        }
        // Stable: each rank keeps the title order of the entries.
        return ranked.sortedBy { it.first }.map { it.second }
    }

    private fun Song.lyricLine(needle: String): String? =
        chart.orEmpty().lyricLines().firstOrNull { needle in it.foldForSearch() }?.trim()

    /** The line holding the most of [words], the first one on a tie. */
    private fun Song.lyricLine(words: List<String>): String? =
        chart.orEmpty().lyricLines()
            .map { line -> line to line.foldForSearch().let { folded -> words.count { it in folded } } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }?.first?.trim()

    companion object {
        /** Shorter words are in nearly every song. */
        private const val MIN_LYRICS_QUERY = 3

        /** Whether a search for [query] also reads the lyrics. */
        fun readsLyrics(query: String): Boolean = query.trim().foldForSearch().length >= MIN_LYRICS_QUERY

        /** The first character of [title] as the search reads it, in upper case; a space when it is empty. */
        fun firstLetter(title: String): Char = title.trim().foldForSearch().firstOrNull()?.uppercaseChar() ?: ' '
    }
}

private val chordToken = Regex("""\[[^\]]*\]""")

/** The words of a chart: no chords, no section titles. */
private fun String.lyricLines(): Sequence<String> =
    lineSequence().filterNot { it.trimStart().startsWith("#") }.map { chordToken.replace(it, "") }.filter { it.isNotBlank() }

private val folded = mapOf(
    'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ç' to 'c', 'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'î' to 'i', 'ï' to 'i', 'ô' to 'o', 'ö' to 'o', 'ù' to 'u', 'û' to 'u', 'ü' to 'u', 'ÿ' to 'y',
    // A typed apostrophe finds the typographic one of an imported chart.
    '’' to '\'',
)

internal fun String.foldForSearch(): String = buildString(length) {
    for (char in this@foldForSearch) {
        val lower = char.lowercaseChar()
        append(folded[lower] ?: lower)
    }
}
