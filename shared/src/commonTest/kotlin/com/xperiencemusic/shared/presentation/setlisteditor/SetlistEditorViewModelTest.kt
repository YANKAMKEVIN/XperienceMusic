package com.xperiencemusic.shared.presentation.setlisteditor

import app.cash.turbine.test
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.usecase.DeleteSetlistUseCase
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.domain.usecase.PublishSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.domain.usecase.SuggestBandWeekUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SetlistEditorViewModelTest {
    private val songs = songRepository(song("a", "Alléluia"), song("b", "Béni"), song("c", "Car tu es saint"))
    private val setlists = setlistRepository()
    private lateinit var viewModel: SetlistEditorViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = editor()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val session = FakeSessionRepository(member(Role.CONTENT_ADMIN))

    private fun editor(today: LocalDate = LocalDate(2026, 10, 10), serviceDate: LocalDate? = null) = SetlistEditorViewModel(
        SearchSongsUseCase(songs),
        PublishSetlistUseCase(session, setlists),
        DeleteSetlistUseCase(session, setlists),
        SuggestBandWeekUseCase { today },
        schedule(setlists, songs, today),
        serviceDate,
    )

    @Test
    fun `la semaine choisie s'ouvre pour être modifiée`() = runTest {
        setlists.publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("c", "a"), "Intro", "x"))
        editor(serviceDate = LocalDate(2026, 10, 18)).state.test {
            val state = expectMostRecentItem()
            assertTrue(state.isEditing)
            assertEquals(listOf("c", "a"), state.selected.map { it.id })
            assertEquals("Intro", state.message)
        }
    }

    @Test
    fun `une nouvelle setlist prend la première semaine encore libre`() = runTest {
        setlists.publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("a"), null, "x"))
        editor().state.test {
            val state = expectMostRecentItem()
            assertFalse(state.isEditing)
            assertEquals(LocalDate(2026, 10, 25), state.serviceDate)
            assertTrue(state.selected.isEmpty())
        }
    }

    @Test
    fun `choisir un dimanche déjà pris prévient du remplacement`() = runTest {
        setlists.publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("a"), null, "x"))
        val viewModel = editor()
        viewModel.state.test {
            viewModel.onEvent(SetlistEditorEvent.ServiceDatePicked(LocalDate(2026, 10, 18)))
            assertTrue(expectMostRecentItem().replacesExisting)
        }
    }

    @Test
    fun `déplacer une semaine vers un autre dimanche libère l'ancien`() = runTest {
        setlists.publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("a"), null, "x"))
        val viewModel = editor(serviceDate = LocalDate(2026, 10, 18))
        viewModel.state.test {
            viewModel.onEvent(SetlistEditorEvent.ServiceDatePicked(LocalDate(2026, 10, 19)))
            viewModel.onEvent(SetlistEditorEvent.PublishClicked)
            assertTrue(expectMostRecentItem().isPublished)
        }
        assertEquals(listOf(LocalDate(2026, 10, 19)), setlists.observeSetlists().first().map { it.serviceDate })
    }

    @Test
    fun `supprimer la semaine la retire pour tout le groupe`() = runTest {
        setlists.publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("a"), null, "x"))
        val viewModel = editor(serviceDate = LocalDate(2026, 10, 18))
        viewModel.state.test {
            expectMostRecentItem()
            viewModel.onEvent(SetlistEditorEvent.DeleteConfirmed)
            assertTrue(expectMostRecentItem().isDeleted)
        }
        assertTrue(setlists.observeSetlists().first().isEmpty())
    }

    @Test
    fun `les dates sont proposées et l'ordre des chants se règle`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertEquals("mardi 13 oct.", initial.rehearsalLabel)
            assertEquals("dimanche 18 oct.", initial.serviceLabel)
            assertFalse(initial.canPublish)

            viewModel.onEvent(SetlistEditorEvent.SongAdded("c"))
            viewModel.onEvent(SetlistEditorEvent.SongAdded("a"))
            viewModel.onEvent(SetlistEditorEvent.SongMovedUp("a"))
            val ordered = expectMostRecentItem()
            assertEquals(listOf("a", "c"), ordered.selected.map { it.id })
            assertEquals(listOf("b"), ordered.available.map { it.id })
            assertTrue(ordered.canPublish)
        }
    }

    @Test
    fun `une prestation avant la répétition bloque la publication`() = runTest {
        viewModel.state.test {
            awaitItem()
            viewModel.onEvent(SetlistEditorEvent.SongAdded("a"))
            viewModel.onEvent(SetlistEditorEvent.ServiceDatePicked(LocalDate(2026, 10, 12)))
            val state = expectMostRecentItem()
            assertTrue(state.hasDateConflict)
            assertFalse(state.canPublish)
        }
    }

    @Test
    fun `publier enregistre la setlist dans l'ordre choisi`() = runTest {
        viewModel.state.test {
            awaitItem()
            viewModel.onEvent(SetlistEditorEvent.SongAdded("b"))
            viewModel.onEvent(SetlistEditorEvent.SongAdded("a"))
            viewModel.onEvent(SetlistEditorEvent.MessageChanged("  Intro douce  "))
            viewModel.onEvent(SetlistEditorEvent.PublishClicked)
            assertTrue(expectMostRecentItem().isPublished)
        }
        val published = setlists.observeSetlists().first().single()
        assertEquals(listOf("b", "a"), published.songIds)
        assertEquals("Intro douce", published.message)
        assertEquals("Grâce", published.publishedBy)
    }
}
