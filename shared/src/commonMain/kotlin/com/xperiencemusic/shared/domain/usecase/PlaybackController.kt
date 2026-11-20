package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.domain.repository.SessionResettable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * The app's single playback session: which track is loaded and how far along it is. Lives as long
 * as the app so listening continues while moving between screens.
 */
class PlaybackController(
    private val player: AudioPlayer,
    private val urls: AudioUrlProvider,
    private val scope: CoroutineScope,
) : SessionResettable {
    private val session = MutableStateFlow(Session())
    private var loading: Job? = null

    /** [queue] is what plays one after the other, [track] being its entry at [index]. */
    private data class Session(
        val track: PlaybackTrack? = null,
        val isResolving: Boolean = false,
        val hasError: Boolean = false,
        val queue: List<PlaybackTrack> = emptyList(),
        val index: Int = 0,
    )

    /** How the track is worked on; the loop belongs to the track, the speed lasts until the player closes. */
    private data class Practice(
        val speed: Float = PlaybackState.NORMAL_SPEED,
        val loopStartMs: Long? = null,
        val loopEndMs: Long? = null,
    )

    private val practice = MutableStateFlow(Practice())

    val state: StateFlow<PlaybackState> = combine(session, player.state, practice) { session, player, practice ->
        PlaybackState(
            track = session.track,
            isPlaying = session.track != null && player.isPlaying,
            isLoading = session.isResolving || (session.track != null && player.isBuffering),
            positionMs = if (session.isResolving) 0 else player.positionMs,
            durationMs = if (session.isResolving) 0 else player.durationMs,
            hasError = session.hasError || (session.track != null && !session.isResolving && player.hasError),
            queueIndex = session.index,
            queueSize = session.queue.size,
            speed = practice.speed,
            loopStartMs = practice.loopStartMs,
            loopEndMs = practice.loopEndMs,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackState())

    init {
        // A setlist plays through: the end of a track starts the next one, screen off included.
        scope.launch {
            player.state.map { it.hasEnded }.distinctUntilChanged().collect { hasEnded ->
                val current = session.value
                // A passage being looped keeps its track: the loop below takes it back to its start.
                if (hasEnded && !current.isResolving && !isLooping && current.index < current.queue.lastIndex) {
                    load(current.queue, current.index + 1)
                }
            }
        }
        // The A-B loop: reaching B (or the end of the track) goes back to A and keeps playing.
        scope.launch {
            player.state.collect { playing ->
                val loop = practice.value
                val start = loop.loopStartMs
                val end = loop.loopEndMs
                if (session.value.track != null && start != null && end != null && (playing.positionMs >= end || playing.hasEnded)) {
                    player.seekTo(start)
                    if (playing.hasEnded) player.resume()
                }
            }
        }
    }

    private val isLooping: Boolean get() = practice.value.let { it.loopStartMs != null && it.loopEndMs != null }

    /** Switches between the recording's tempo and the slow one, at the same pitch. */
    fun toggleSpeed() {
        if (session.value.track == null) return
        val next = if (practice.value.speed < PlaybackState.NORMAL_SPEED) PlaybackState.NORMAL_SPEED else PlaybackState.SLOW_SPEED
        practice.update { it.copy(speed = next) }
        player.setSpeed(next)
    }

    /**
     * The loop button, pressed up to three times: marks A where the track is, then B (the passage
     * then repeats), then removes the loop. A "B" too close to A, or before it, is ignored.
     */
    fun stepLoop() {
        val current = state.value
        if (current.track == null || current.isLoading) return
        val position = current.positionMs
        val before = practice.value
        val after = when {
            before.loopStartMs == null -> before.copy(loopStartMs = position)
            before.loopEndMs == null ->
                if (position - before.loopStartMs >= MIN_LOOP_MS) before.copy(loopEndMs = position) else before
            else -> before.copy(loopStartMs = null, loopEndMs = null)
        }
        practice.value = after
        // B was just marked: the passage starts over at once, which also shows the loop is on.
        if (before.loopEndMs == null && after.loopEndMs != null) after.loopStartMs?.let(player::seekTo)
    }

    /**
     * Plays [track]; on the track already loaded, toggles pause instead. A track of the list being
     * played through keeps that list and jumps to its place in it; any other track plays alone.
     */
    fun playOrToggle(track: PlaybackTrack) {
        val current = state.value
        if (current.track?.id == track.id && !current.hasError) {
            togglePause()
            return
        }
        val queue = session.value.queue
        val place = queue.indexOfFirst { it.id == track.id }
        if (place >= 0) load(queue, place) else load(listOf(track), 0)
    }

    /** Plays [tracks] one after the other, from [startIndex]: the whole setlist in one go. */
    fun playAll(tracks: List<PlaybackTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        load(tracks, startIndex.coerceIn(0, tracks.lastIndex))
    }

    /** The next track of the list; nothing on the last one or on a single track. */
    fun next() {
        val current = session.value
        if (current.index < current.queue.lastIndex) load(current.queue, current.index + 1)
    }

    /**
     * Back to the start of the track, like any music player; pressed again within the first
     * seconds, the previous track of the list.
     */
    fun previous() {
        val current = session.value
        if (current.track == null) return
        if (current.index > 0 && state.value.positionMs < RESTART_THRESHOLD_MS) {
            load(current.queue, current.index - 1)
        } else {
            seekTo(0)
        }
    }

    private fun load(queue: List<PlaybackTrack>, index: Int) {
        val track = queue[index]
        loading?.cancel()
        player.stop()
        // A passage belongs to its recording: another track starts without the loop.
        practice.update { it.copy(loopStartMs = null, loopEndMs = null) }
        session.value = Session(track = track, isResolving = true, queue = queue, index = index)
        loading = scope.launch {
            try {
                val url = urls.playableUrl(track.audioPath)
                session.update { it.copy(isResolving = false) }
                player.play(url, NowPlaying(track.title, track.subtitle))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                session.update { it.copy(isResolving = false, hasError = true) }
            }
        }
    }

    fun togglePause() {
        val current = state.value
        when {
            current.track == null || current.isLoading -> Unit
            current.isPlaying -> player.pause()
            // At the end, start over.
            player.state.value.hasEnded -> {
                player.seekTo(0)
                player.resume()
            }
            else -> player.resume()
        }
    }

    fun seekTo(positionMs: Long) {
        if (state.value.track != null) player.seekTo(positionMs.coerceAtLeast(0))
    }

    /** The next member must not find the previous one's track loaded. */
    override fun resetSession() = stop()

    fun stop() {
        loading?.cancel()
        player.stop()
        session.value = Session()
        // The player is shared with the exercises and the voice recorder: they get the normal speed back.
        practice.value = Practice()
        player.setSpeed(PlaybackState.NORMAL_SPEED)
    }

    private companion object {
        /** Past this point "previous" restarts the track instead of leaving it. */
        const val RESTART_THRESHOLD_MS = 3_000L

        /** Shorter than this, a loop is a stutter: the second press is taken for a slip. */
        const val MIN_LOOP_MS = 1_000L
    }
}
