package com.xperiencemusic.shared.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.presentation.common.shortLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    searchSongs: SearchSongsUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val refreshContent: RefreshContentUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedKeyLabel = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)

    private val keys = searchSongs("").map { deriveKeyFilters(it) }

    /** The chosen key, dropped once no song is in it any more or the filter is hidden. */
    private val selectedKey = combine(keys, selectedKeyLabel) { keys, label ->
        if (keys.size < 2) null else keys.firstOrNull { it.shortLabel() == label }
    }

    private val results = combine(query, selectedKey) { query, key -> query to key }
        .flatMapLatest { (query, key) -> searchSongs(query, key) }
        .map { songs -> songs.map { it.toLibraryItem() } }

    private val filters = combine(query, keys, selectedKey) { query, keys, key -> Triple(query, keys, key) }

    val state: StateFlow<LibraryUiState> = combine(
        filters,
        results,
        observeCurrentMember(),
        observeSyncStatus(),
        isRefreshing,
    ) { (query, keys, key), songs, member, sync, refreshing ->
        LibraryUiState(
            query = query,
            keys = if (keys.size < 2) emptyList() else keys.map { it.shortLabel() },
            selectedKey = key?.shortLabel(),
            isLoading = false,
            songs = songs,
            canAddSong = member?.role?.can(Permission.EDIT_SONG) == true,
            isRefreshing = refreshing,
            isOffline = sync == SyncStatus.FAILED,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.QueryChanged -> query.value = event.query
            is LibraryEvent.KeySelected -> selectedKeyLabel.value = event.key
            LibraryEvent.RefreshPulled -> refresh()
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            refreshContent()
            isRefreshing.value = false
        }
    }
}
