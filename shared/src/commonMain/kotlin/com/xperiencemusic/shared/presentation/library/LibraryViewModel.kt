package com.xperiencemusic.shared.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    searchSongs: SearchSongsUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val results = query.flatMapLatest { searchSongs(it) }.map { songs -> songs.map { it.toLibraryItem() } }

    val state: StateFlow<LibraryUiState> = combine(query, results, observeCurrentMember()) { query, songs, member ->
        LibraryUiState(
            query = query,
            isLoading = false,
            songs = songs,
            canAddSong = member?.role?.can(Permission.EDIT_SONG) == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.QueryChanged -> query.value = event.query
        }
    }
}
