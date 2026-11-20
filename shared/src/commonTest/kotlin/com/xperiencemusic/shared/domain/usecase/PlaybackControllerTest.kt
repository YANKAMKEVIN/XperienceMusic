package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.NowPlaying
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
        assertEquals(listOf(NowPlaying("Référence", "À toi la gloire")), player.nowPlaying)
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
    fun `une setlist s'enchaîne toute seule jusqu'au dernier chant`() = runTest {
        val controller = controllerWith()
        controller.playAll(listOf(reference, acoustic))
        assertEquals("r1", controller.state.value.track?.id)
        assertEquals(0, controller.state.value.queueIndex)
        assertEquals(2, controller.state.value.queueSize)
        assertTrue(controller.state.value.hasNext)

        player.finish()
        assertEquals("r2", controller.state.value.track?.id)
        assertTrue(controller.state.value.isPlaying)
        assertFalse(controller.state.value.hasNext)

        // The last one ends like a single track: loaded, stopped, ready to start over.
        player.finish()
        assertEquals("r2", controller.state.value.track?.id)
        assertFalse(controller.state.value.isPlaying)
        assertEquals(2, player.played.size)
    }

    @Test
    fun `suivant et précédent parcourent la setlist`() = runTest {
        val controller = controllerWith()
        controller.playAll(listOf(reference, acoustic))
        controller.next()
        assertEquals("r2", controller.state.value.track?.id)
        controller.next()
        assertEquals("r2", controller.state.value.track?.id, "nothing after the last track")

        // Well into the track, "previous" starts it over; pressed again at once, it goes back.
        player.seekTo(40_000)
        controller.previous()
        assertEquals("r2", controller.state.value.track?.id)
        assertEquals(0, player.state.value.positionMs)
        controller.previous()
        assertEquals("r1", controller.state.value.track?.id)
        assertFalse(controller.state.value.hasPrevious)
    }

    @Test
    fun `toucher un chant de la setlist en cours y saute sans la quitter`() = runTest {
        val third = PlaybackTrack("r3", "Référence", "Ta lumière", "songs/s2/r3.m4a")
        val controller = controllerWith()
        controller.playAll(listOf(reference, acoustic, third))
        controller.playOrToggle(third)
        assertEquals(2, controller.state.value.queueIndex)
        assertEquals(3, controller.state.value.queueSize)

        // A track from elsewhere plays alone again.
        controller.playOrToggle(PlaybackTrack("v1", "Alto", "Ta lumière", "voices/v1.m4a"))
        assertEquals(1, controller.state.value.queueSize)
        player.finish()
        assertEquals("v1", controller.state.value.track?.id)
    }

    @Test
    fun `la vitesse lente se règle sur le lecteur et se remet à la fermeture`() = runTest {
        val controller = controllerWith()
        controller.toggleSpeed()
        assertEquals(1f, player.playbackSpeed, "nothing is loaded: nothing to slow down")

        controller.playOrToggle(reference)
        controller.toggleSpeed()
        assertEquals(PlaybackState.SLOW_SPEED, player.playbackSpeed)
        assertTrue(controller.state.value.isSlowed)
        // The next track keeps the working tempo.
        controller.playOrToggle(acoustic)
        assertTrue(controller.state.value.isSlowed)

        controller.stop()
        assertEquals(PlaybackState.NORMAL_SPEED, player.playbackSpeed)
        assertFalse(controller.state.value.isSlowed)
    }

    @Test
    fun `la boucle A-B rejoue le passage jusqu'à ce qu'on l'enlève`() = runTest {
        val controller = controllerWith()
        controller.playOrToggle(reference)
        player.advanceTo(42_000)
        controller.stepLoop()
        assertEquals(42_000, controller.state.value.loopStartMs)
        assertNull(controller.state.value.loopEndMs)

        // A second press at the same place is a slip, not a loop.
        player.advanceTo(42_300)
        controller.stepLoop()
        assertNull(controller.state.value.loopEndMs)

        player.advanceTo(55_000)
        controller.stepLoop()
        assertEquals(55_000, controller.state.value.loopEndMs)
        assertEquals(42_000, player.state.value.positionMs, "reaching B goes back to A")
        player.advanceTo(50_000)
        assertEquals(50_000, player.state.value.positionMs)
        player.advanceTo(55_200)
        assertEquals(42_000, player.state.value.positionMs)
        assertTrue(controller.state.value.isPlaying)

        controller.stepLoop()
        assertNull(controller.state.value.loopStartMs)
        player.advanceTo(60_000)
        assertEquals(60_000, player.state.value.positionMs)
    }

    @Test
    fun `une boucle ne suit pas sur le chant suivant et retient la setlist`() = runTest {
        val controller = controllerWith()
        controller.playAll(listOf(reference, acoustic))
        player.advanceTo(190_000)
        controller.stepLoop()
        player.advanceTo(199_000)
        controller.stepLoop()
        // The loop reaches the end of the track: it starts over instead of moving on.
        player.finish()
        assertEquals("r1", controller.state.value.track?.id)
        assertEquals(190_000, player.state.value.positionMs)

        controller.next()
        assertEquals("r2", controller.state.value.track?.id)
        assertNull(controller.state.value.loopStartMs)
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
