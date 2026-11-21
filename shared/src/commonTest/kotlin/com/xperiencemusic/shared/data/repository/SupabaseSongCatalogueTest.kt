package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.fakes.FakeFileStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The list of songs is loaded without the charts, and a chart arrives when it is needed. A fake
 * server holds the `songs` table and answers the selects as PostgREST would: only the asked
 * columns, by pages, filtered by id.
 */
class SupabaseSongCatalogueTest {
    /** A fake `songs` table: [count] songs, the lyrics of song n saying "Ligne n". */
    private class FakeSongTable(count: Int, private val hasThemesColumn: Boolean = false) {
        val rows = (1..count).map { n ->
            val id = "song-${n.toString().padStart(5, '0')}"
            mutableMapOf<String, JsonElement>(
                "id" to JsonPrimitive(id),
                "title" to JsonPrimitive("Chant $n"),
                "artist" to JsonPrimitive("Auteur $n"),
                "key_tonic" to JsonPrimitive(n % 12),
                "key_mode" to JsonPrimitive("MAJOR"),
                "chart" to JsonPrimitive("# Couplet\n[1]Sei[4]gneur, ligne $n"),
                "recordings" to JsonArray(emptyList()),
                "voice_tracks" to JsonArray(emptyList()),
                "song_links" to JsonArray(emptyList()),
            ).also { if (hasThemesColumn) it["themes"] = buildJsonArray { if (n % 2 == 0) add(JsonPrimitive("adoration")) } }
        }
        val selects = mutableListOf<String>()
        var offline = false

        fun answer(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData {
            if (offline) throw IOException("network dropped")
            val params = request.url.parameters
            val columns = params["select"].orEmpty().replace(" ", "").split(',')
                .map { it.substringBefore('(') }
            selects += columns.joinToString(",")
            if (columns.any { it !in rows.first() }) {
                return scope.respond(
                    content = """{"code":"42703","message":"column songs.${columns.first { it !in rows.first() }} does not exist"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
            var selected = rows.sortedBy { (it["id"] as JsonPrimitive).content }
            params["id"]?.removePrefix("in.(")?.removeSuffix(")")?.split(',')?.map { it.trim('"') }?.let { ids ->
                selected = selected.filter { (it["id"] as JsonPrimitive).content in ids }
            }
            val (from, to) = range(request)
            selected = selected.drop(from).take(to - from + 1)
            val body = JsonArray(selected.map { row -> JsonObject(row.filterKeys { it in columns }) })
            return scope.respond(
                content = Json.encodeToString(JsonArray.serializer(), body),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        /** The rows asked for, by the `offset` and `limit` parameters or the `Range` header. */
        private fun range(request: HttpRequestData): Pair<Int, Int> {
            val params = request.url.parameters
            params["offset"]?.let { offset ->
                val limit = params["limit"]?.toInt() ?: Int.MAX_VALUE
                return offset.toInt() to (offset.toInt() + limit - 1)
            }
            request.headers["Range"]?.split('-')?.let { (from, to) -> return from.toInt() to to.toInt() }
            return 0 to (SERVER_ROW_LIMIT - 1)
        }
    }

    private fun client(table: FakeSongTable): SupabaseClient =
        createSupabaseClient(supabaseUrl = "https://test.supabase.co", supabaseKey = "test-key") {
            httpEngine = MockEngine { request -> table.answer(this, request) }
            install(Postgrest)
            install(Storage)
        }

    private val files = FakeFileStore()
    private fun repository(table: FakeSongTable) = SupabaseSongRepository(client(table), SnapshotStore(files))

    @Test
    fun `la liste charge tous les chants par pages, sans leurs paroles`() = runTest {
        val table = FakeSongTable(count = 2500)

        val songs = repository(table).observeSongs().first()

        assertEquals(2500, songs.size)
        assertTrue(songs.all { it.chart == null })
        assertEquals("Auteur 7", songs.first { it.title == "Chant 7" }.artist)
        assertTrue(table.selects.isNotEmpty())
        assertTrue(table.selects.none { "chart" in it.split(',') }, "list selects: ${table.selects}")
    }

    @Test
    fun `ouvrir la fiche charge les paroles de ce seul chant`() = runTest {
        val table = FakeSongTable(count = 30)
        val repository = repository(table)
        repository.observeSongs().first()
        table.selects.clear()

        val song = repository.observeSong("song-00007").first { it?.chart != null }

        assertEquals("# Couplet\n[1]Sei[4]gneur, ligne 7", song?.chart)
        assertEquals(listOf("id,chart"), table.selects)
        // The list now has that chart, and that one only.
        val list = repository.observeSongs().first()
        assertEquals(listOf("song-00007"), list.filter { it.chart != null }.map { it.id })
    }

    @Test
    fun `une recherche de 3 lettres lit les paroles une seule fois, chords et accents compris`() = runTest {
        val table = FakeSongTable(count = 1500)
        val repository = repository(table)
        val search = SearchSongsUseCase(repository)
        repository.observeSongs().first()
        assertTrue(search.index().first().search("seigneur, ligne 1234", inLyrics = true).isEmpty())

        search.loadLyrics()
        search.loadLyrics()

        val found = search.index().first().search("Séigneur, ligne 1234", inLyrics = true)
        assertEquals(listOf("Chant 1234"), found.map { it.song.title })
        assertEquals("Seigneur, ligne 1234", found.single().lyricLine)
        // Fetched once: two full pages, then the empty one that ends the list.
        assertEquals(3, table.selects.count { it == "id,chart" })
    }

    @Test
    fun `les paroles déjà lues restent sur le téléphone, réseau coupé`() = runTest {
        val table = FakeSongTable(count = 10)
        repository(table).observeSong("song-00003").first { it?.chart != null }

        table.offline = true
        val offline = repository(table)

        assertEquals("# Couplet\n[1]Sei[4]gneur, ligne 3", offline.observeSong("song-00003").first()?.chart)
        assertNull(offline.observeSong("song-00004").first()?.chart)
    }

    @Test
    fun `la copie d'une version précédente garde ses paroles hors ligne`() = runTest {
        // Earlier versions saved the list with every chart in it.
        files.writeText(
            "snapshots/songs.json",
            """[{"id":"s1","title":"Gloire","key_tonic":0,"key_mode":"MAJOR","chart":"[1]Gloire à toi"}]""",
        )
        val table = FakeSongTable(count = 0).apply { offline = true }

        val song = repository(table).observeSong("s1").first()

        assertEquals("[1]Gloire à toi", song?.chart)
    }

    @Test
    fun `télécharger la semaine charge les paroles de ses chants`() = runTest {
        val table = FakeSongTable(count = 20)
        val repository = repository(table)

        repository.loadCharts(listOf("song-00002", "song-00005"))

        val withChart = repository.observeSongs().first().filter { it.chart != null }.map { it.id }
        assertEquals(listOf("song-00002", "song-00005"), withChart.sorted())
    }

    private companion object {
        /** Supabase's default row limit, as the app's pages assume. */
        const val SERVER_ROW_LIMIT = 1000
    }
}
