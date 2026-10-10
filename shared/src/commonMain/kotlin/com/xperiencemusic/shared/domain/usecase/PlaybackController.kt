package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) {
    private val session = MutableStateFlow(Session())
    private var loading: Job? = null

    private data class Session(
        val track: PlaybackTrack? = null,
        val isResolving: Boolean = false,
        val hasError: Boolean = false,
    )

    val state: StateFlow<PlaybackState> = combine(session, player.state) { session, player ->
        PlaybackState(
            track = session.track,
            isPlaying = session.track != null && player.isPlaying,
            isLoading = session.isResolving || (session.track != null && player.isBuffering),
            positionMs = if (session.isResolving) 0 else player.positionMs,
            durationMs = if (session.isResolving) 0 else player.durationMs,
            hasError = session.hasError || (session.track != null && !session.isResolving && player.hasError),
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackState())

    /** Plays [track]; on the track already loaded, toggles pause instead. */
    fun playOrToggle(track: PlaybackTrack) {
        val current = state.value
        if (current.track?.id == track.id && !current.hasError) {
            togglePause()
            return
        }
        loading?.cancel()
        player.stop()
        session.value = Session(track = track, isResolving = true)
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

    fun stop() {
        loading?.cancel()
        player.stop()
        session.value = Session()
    }
}
