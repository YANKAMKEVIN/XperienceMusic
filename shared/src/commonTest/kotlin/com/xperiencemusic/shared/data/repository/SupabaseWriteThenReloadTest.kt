package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.fakes.FakeFileStore
import com.xperiencemusic.shared.fakes.recording
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * A write that the server accepted must be reported as a success even when the reload that
 * follows it fails (network drop right after the write): otherwise the user retries and creates a
 * duplicate, and an uploaded recording's audio file is deleted although its row exists.
 */
class SupabaseWriteThenReloadTest {
    private val writes = mutableListOf<String>()

    /** Writes (POST/PATCH/DELETE) succeed; every read (GET) fails as if the network dropped. */
    private fun client(failWrites: Boolean = false): SupabaseClient =
        createSupabaseClient(supabaseUrl = "https://test.supabase.co", supabaseKey = "test-key") {
            httpEngine = MockEngine { request -> handle(request, failWrites) }
            install(Postgrest)
            install(Storage)
        }

    private fun MockRequestHandleScope.handle(request: HttpRequestData, failWrites: Boolean): HttpResponseData {
        if (request.method == HttpMethod.Get) throw IOException("network dropped")
        if (failWrites) throw IOException("network dropped")
        writes += "${request.method.value} ${request.url.encodedPath}"
        return respond(
            content = """[{"id":"new-song"}]""",
            status = HttpStatusCode.Created,
            headers = headersOf("Content-Type", "application/json"),
        )
    }

    private val snapshots = SnapshotStore(FakeFileStore())
    private val draft = SongDraft("Chant", null, Key(0), "[1]Gloire")

    @Test
    fun `createSong returns the new id when the reload after the insert fails`() = runTest {
        val repository = SupabaseSongRepository(client(), snapshots)

        val id = repository.createSong(draft)

        assertEquals("new-song", id)
        assertEquals(listOf("POST /rest/v1/songs"), writes)
        assertEquals(SyncStatus.FAILED, repository.syncStatus.value)
    }

    @Test
    fun `addRecording succeeds when the reload after the insert fails`() = runTest {
        val repository = SupabaseSongRepository(client(), snapshots)

        repository.addRecording("s1", recording("r1", day = 3), replacingId = null)

        assertEquals(listOf("POST /rest/v1/rpc/add_recording"), writes)
    }

    @Test
    fun `a failed write is still reported as a failure`() = runTest {
        val repository = SupabaseSongRepository(client(failWrites = true), snapshots)

        assertFailsWith<Exception> { repository.createSong(draft) }
    }

    @Test
    fun `publishing a setlist succeeds when the reload after it fails`() = runTest {
        val repository = SupabaseSetlistRepository(client(), snapshots)

        repository.publish(Setlist("w1", LocalDate(2026, 10, 8), LocalDate(2026, 10, 11), listOf("s1"), null, "me"))

        assertEquals(listOf("POST /rest/v1/rpc/publish_setlist"), writes)
    }

    @Test
    fun `an invitation succeeds when the reload after it fails, and the reload stays retryable`() = runTest {
        val loads = RemoteLoadMonitor()
        val repository = SupabaseMembersRepository(client(), loads)

        repository.invite("new@band.org", Role.MEMBER)

        assertEquals(listOf("POST /rest/v1/invitations"), writes)
        assertTrue(loads.hasFailure.value)
    }
}
