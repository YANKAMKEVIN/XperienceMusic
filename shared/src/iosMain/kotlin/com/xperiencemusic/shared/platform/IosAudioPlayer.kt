package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AudioPlayerState
import com.xperiencemusic.shared.domain.repository.NowPlaying
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

/**
 * AVPlayer-backed output. State is sampled every [TICK_MS] rather than observed through KVO, which
 * keeps the interop simple and is plenty for a progress bar.
 *
 * With the "audio" background mode (Info.plist) playback continues with the screen locked; the
 * lock screen and Control Center show [NowPlaying] and drive the player through remote commands.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer : AudioPlayer {
    private val _state = MutableStateFlow(AudioPlayerState())
    override val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val scope = MainScope()
    private var player: AVPlayer? = null
    private var ticker: Job? = null
    private var nowPlaying: NowPlaying? = null
    private var lastPublishedKey = -1L

    init {
        registerRemoteCommands()
    }

    override fun play(url: String, nowPlaying: NowPlaying) {
        stop()
        val nsUrl = NSURL.URLWithString(url) ?: run {
            _state.value = AudioPlayerState(hasError = true)
            return
        }
        activateSession()
        this.nowPlaying = nowPlaying
        lastPublishedKey = -1
        val newPlayer = AVPlayer(playerItem = AVPlayerItem(uRL = nsUrl))
        player = newPlayer
        _state.value = AudioPlayerState(isBuffering = true)
        newPlayer.play()
        ticker = scope.launch {
            while (isActive) {
                sample(newPlayer)
                delay(TICK_MS)
            }
        }
    }

    override fun pause() {
        player?.pause()
        player?.let(::sample)
    }

    override fun resume() {
        player?.play()
        player?.let(::sample)
    }

    override fun seekTo(positionMs: Long) {
        val current = player ?: return
        current.seekToTime(CMTimeMakeWithSeconds(positionMs / MILLIS_PER_SECOND, TIMESCALE))
        _state.value = _state.value.copy(positionMs = positionMs, hasEnded = false)
    }

    override fun stop() {
        ticker?.cancel()
        ticker = null
        player?.pause()
        player = null
        nowPlaying = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
        _state.value = AudioPlayerState()
    }

    private fun sample(current: AVPlayer) {
        val item = current.currentItem
        val position = CMTimeGetSeconds(current.currentTime()).toMillisOrZero()
        val duration = item?.let { CMTimeGetSeconds(it.duration).toMillisOrZero() } ?: 0
        val isPlaying = current.timeControlStatus == AVPlayerTimeControlStatusPlaying
        _state.value = AudioPlayerState(
            isPlaying = isPlaying,
            isBuffering = current.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate,
            positionMs = position,
            durationMs = duration,
            hasEnded = !isPlaying && duration > 0 && position >= duration - END_TOLERANCE_MS,
            hasError = item?.status == AVPlayerItemStatusFailed,
        )
        publishNowPlaying(position, duration, isPlaying)
    }

    /** Lock-screen info; refreshed when the second changes or play state flips. */
    private fun publishNowPlaying(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        val info = nowPlaying ?: return
        // Changes once per second of playback, or when play/pause flips.
        val key = positionMs / 1000 * 2 + if (isPlaying) 1 else 0
        if (key == lastPublishedKey) return
        lastPublishedKey = key
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to info.title,
            MPMediaItemPropertyArtist to info.subtitle,
            MPMediaItemPropertyPlaybackDuration to durationMs / MILLIS_PER_SECOND,
            MPNowPlayingInfoPropertyElapsedPlaybackTime to positionMs / MILLIS_PER_SECOND,
            MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) 1.0 else 0.0,
        )
    }

    private fun registerRemoteCommands() {
        val commands = MPRemoteCommandCenter.sharedCommandCenter()
        commands.playCommand.addTargetWithHandler { _ ->
            resume()
            MPRemoteCommandHandlerStatusSuccess
        }
        commands.pauseCommand.addTargetWithHandler { _ ->
            pause()
            MPRemoteCommandHandlerStatusSuccess
        }
        commands.togglePlayPauseCommand.addTargetWithHandler { _ ->
            if (_state.value.isPlaying) pause() else resume()
            MPRemoteCommandHandlerStatusSuccess
        }
        commands.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val position = (event as? MPChangePlaybackPositionCommandEvent)?.positionTime
            if (position != null) seekTo((position * MILLIS_PER_SECOND).toLong())
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    /** Plays through the silent switch, like any music app. */
    private fun activateSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    private fun Double.toMillisOrZero(): Long = if (isFinite() && this > 0) (this * MILLIS_PER_SECOND).toLong() else 0

    private companion object {
        const val TICK_MS = 250L
        const val MILLIS_PER_SECOND = 1000.0
        const val TIMESCALE = 600
        const val END_TOLERANCE_MS = 300
    }
}
