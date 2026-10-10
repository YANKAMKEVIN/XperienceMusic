package com.xperiencemusic.shared.platform

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AudioPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** ExoPlayer-backed output. Must be driven from the main thread, as ExoPlayer requires. */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private val _state = MutableStateFlow(AudioPlayerState())
    override val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startTicker() else publishPosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    hasEnded = playbackState == Player.STATE_ENDED,
                )
            }
            publishPosition()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(hasError = true, isPlaying = false, isBuffering = false) }
        }
    }

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                // Pause when another app takes the audio focus (call, other music).
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(listener) }
    }

    override fun play(url: String) {
        _state.value = AudioPlayerState(isBuffering = true)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(hasEnded = false) }
        publishPosition()
    }

    override fun stop() {
        ticker?.cancel()
        player.stop()
        player.clearMediaItems()
        _state.value = AudioPlayerState()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && player.isPlaying) {
                publishPosition()
                delay(TICK_MS)
            }
        }
    }

    private fun publishPosition() {
        _state.update {
            it.copy(positionMs = player.currentPosition, durationMs = player.duration.takeIf { d -> d > 0 } ?: 0)
        }
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
