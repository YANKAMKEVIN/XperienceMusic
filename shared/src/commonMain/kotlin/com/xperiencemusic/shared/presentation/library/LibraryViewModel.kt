package com.xperiencemusic.shared.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveRepertoireUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    searchSongs: SearchSongsUseCase,
    observeRepertoire: ObserveRepertoireUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val refreshContent: RefreshContentUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    /** Null until the user picks a tab. */
    private val chosenTab = MutableStateFlow<LibraryTab?>(null)
    private val isRefreshing = MutableStateFlow(false)

    // The index is built once per catalogue, not once per keystroke. Null until the phone has a
    // copy of the songs: the catalogue publishes nothing before, and the screen would wait on it
    // forever when the first load fails.
    private val content: Flow<LibraryUiState?> =
        combine(searchSongs.index(), observeRepertoire(), query, chosenTab, ::libraryContent)
            .map<LibraryUiState, LibraryUiState?> { it }
            .onStart { emit(null) }

    val state: StateFlow<LibraryUiState> = combine(
        content,
        query,
        observeCurrentMember(),
        observeSyncStatus(),
        isRefreshing,
    ) { content, query, member, sync, refreshing ->
        val hasFailed = sync == SyncStatus.FAILED
        (content ?: LibraryUiState(query = query, isLoading = !hasFailed, hasLoadFailed = hasFailed)).copy(
            canAddSong = member?.role?.can(Permission.EDIT_SONG) == true,
            isRefreshing = refreshing,
            isOffline = content != null && hasFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.QueryChanged -> query.value = event.query
            is LibraryEvent.TabSelected -> chosenTab.value = event.tab
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
