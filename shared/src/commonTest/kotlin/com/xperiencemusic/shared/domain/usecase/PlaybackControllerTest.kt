package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerTest {
    private val player = FakeAudioPlayer()
    private val reference = PlaybackTrack("r1", "Référence", "À toi la gloire", "songs/s1/r1.m4a")
    private val acoustic = PlaybackTrack("r2", "Acoustique", "À toi la gloire", "songs/s1/r2.m4a")

    private fun controllerWith(urls: AudioUrlProvider = AudioUrlProvider { "https://cdn/$it" }) =
        kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher()).let { PlaybackController(player, urls, it) }

    @Test
    fun `jouer une piste résout son lien puis lance la lecture`() = runTest {
        val controller = controllerWith()
        controller.playOrToggle(reference)
        assertEquals(listOf("https://cdn/songs/s1/r1.m4a"), player.played)
        val state = controller.state.value
        assertEquals("r1", state.track?.id)
        assertTrue(state.isPlaying)
        assertEquals(200_000, state.durationMs)
    }

    @Test
    fun `retoucher la même piste met en pause puis reprend`() = runTest {
        val controller = controllerWith()
        controller.playOrToggle(reference)
        controller.playOrToggle(reference)
        assertFalse(controller.state.value.isPlaying)
        controller.playOrToggle(reference)
        assertTrue(controller.state.value.isPlaying)
        assertEquals(1, player.played.size, "the same track is not reloaded")
    }

    @Test
    fun `une autre piste remplace la précédente`() = runTest {
        val controller = controllerWith()
        controller.playOrToggle(reference)
        controller.playOrToggle(acoustic)
        assertEquals("r2", controller.state.value.track?.id)
        assertEquals(2, player.played.size)
    }

    @Test
    fun `à la fin, lecture recommence au début`() = runTest {
        val controller = controllerWith()
        controller.playOrToggle(reference)
        player.finish()
        controller.togglePause()
        assertEquals(0, player.state.value.positionMs)
        assertTrue(controller.state.value.isPlaying)
    }

    @Test
    fun `un lien introuvable affiche une erreur sans lancer le lecteur`() = runTest {
        val controller = controllerWith { error("offline") }
        controller.playOrToggle(reference)
        assertTrue(controller.state.value.hasError)
        assertTrue(player.played.isEmpty())
        // Retrying the same track after an error loads it again instead of toggling.
        val retry = PlaybackController(player, { "https://cdn/$it" }, kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher()))
        retry.playOrToggle(reference)
        assertEquals(1, player.played.size)
    }

    @Test
    fun `fermer arrête tout`() = runTest {
        val controller = controllerWith()
        controller.playOrToggle(reference)
        controller.stop()
        assertNull(controller.state.value.track)
        assertFalse(controller.state.value.isPlaying)
    }
}
