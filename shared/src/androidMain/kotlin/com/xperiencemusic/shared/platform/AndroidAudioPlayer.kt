package com.xperiencemusic.shared.platform

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AudioPlayerState
import com.xperiencemusic.shared.domain.repository.NowPlaying
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
import java.io.ByteArrayOutputStream

/**
 * ExoPlayer-backed output, shared with [PlaybackService] which shows it to the system. Must be
 * driven from the main thread, as ExoPlayer requires.
 */
class AndroidAudioPlayer(
    private val context: Context,
    private val player: ExoPlayer,
) : AudioPlayer {
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

    /** The XM monogram (the app icon) as the cover of the media notification and lock screen. */
    private val artwork: ByteArray? by lazy {
        runCatching {
            val size = ARTWORK_SIZE_PX
            val icon = context.packageManager.getApplicationIcon(context.packageName)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size)
            icon.draw(Canvas(bitmap))
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        }.getOrNull()
    }

    /** Connecting a controller binds [PlaybackService]; it is kept for the app's lifetime. */
    private var serviceConnection: ListenableFuture<MediaController>? = null

    init {
        player.addListener(listener)
    }

    override fun play(url: String, nowPlaying: NowPlaying) {
        connectToService()
        _state.value = AudioPlayerState(isBuffering = true)
        val metadata = MediaMetadata.Builder().setTitle(nowPlaying.title).setArtist(nowPlaying.subtitle)
            .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()
        player.setMediaItem(MediaItem.Builder().setUri(url).setMediaMetadata(metadata).build())
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

    private fun connectToService() {
        if (serviceConnection != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        serviceConnection = MediaController.Builder(context, token).buildAsync()
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

    companion object {
        private const val TICK_MS = 250L
        private const val ARTWORK_SIZE_PX = 256

        fun buildExoPlayer(context: Context): ExoPlayer =
            ExoPlayer.Builder(context)
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                    // Pause when another app takes the audio focus (call, other music).
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
    }
}
