package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncStatusTest {
    private val songStatus = MutableStateFlow(SyncStatus.UP_TO_DATE)
    private val setlistStatus = MutableStateFlow(SyncStatus.UP_TO_DATE)
    private val songs = object : SongRepository by songRepository() {
        override val syncStatus = songStatus
        override suspend fun refresh() = error("offline")
    }
    private val setlists = object : SetlistRepository by setlistRepository() {
        override val syncStatus = setlistStatus
    }

    @Test
    fun `un seul échec suffit à signaler le mode hors connexion`() = runTest {
        val status = ObserveSyncStatusUseCase(songs, setlists)
        assertEquals(SyncStatus.UP_TO_DATE, status().first())
        setlistStatus.value = SyncStatus.SYNCING
        assertEquals(SyncStatus.SYNCING, status().first())
        songStatus.value = SyncStatus.FAILED
        assertEquals(SyncStatus.FAILED, status().first())
    }

    @Test
    fun `actualiser sans réseau renvoie une erreur réseau`() = runTest {
        assertEquals(DomainResult.Failure(DomainError.NETWORK), RefreshContentUseCase(songs, setlists)())
    }
}
