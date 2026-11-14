package com.xperiencemusic.shared.di

import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.data.remote.AccordsAppPageSource
import com.xperiencemusic.shared.domain.repository.SongPageSource
import com.xperiencemusic.shared.domain.usecase.AddRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.DeleteRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.DeleteSongUseCase
import com.xperiencemusic.shared.domain.usecase.ImportSongFromLinkUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveOfflineAudioUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.SaveSongUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.domain.usecase.SetReferenceRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.UploadRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.UploadVoiceTrackUseCase
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormViewModel
import com.xperiencemusic.shared.presentation.rehearsal.RehearsalUploadViewModel
import com.xperiencemusic.shared.presentation.song.SongDetailViewModel
import com.xperiencemusic.shared.presentation.songeditor.SongEditorViewModel
import com.xperiencemusic.shared.presentation.stage.StageSettingsStore
import com.xperiencemusic.shared.presentation.stage.StageViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The library: songs, their recordings, the editor and the stage view. */
val songsModule = module {
    single<SongPageSource> { AccordsAppPageSource(get()) }
    factoryOf(::SearchSongsUseCase)
    factoryOf(::ObserveSongUseCase)
    factoryOf(::SaveSongUseCase)
    factoryOf(::ImportSongFromLinkUseCase)
    factoryOf(::DeleteSongUseCase)
    factoryOf(::AddRecordingUseCase)
    factoryOf(::UploadRecordingUseCase)
    factoryOf(::UploadVoiceTrackUseCase)
    factoryOf(::DeleteRecordingUseCase)
    factoryOf(::SetReferenceRecordingUseCase)
    factoryOf(::ObserveOfflineAudioUseCase)
    single { StageSettingsStore() }
    viewModelOf(::LibraryViewModel)
    viewModelOf(::SongDetailViewModel)
    // The rehearsal date is optional (null = a take added from the song), which the constructor DSL cannot resolve.
    viewModel { params -> RecordingFormViewModel(params.get(), get(), get(), get(), get(), params.getOrNull()) }
    viewModel { params -> RehearsalUploadViewModel(params.get(), get()) }
    // The song id is optional (null = new song), which the constructor DSL cannot resolve.
    viewModel { params -> SongEditorViewModel(params.getOrNull<String>(), get(), get(), get(), get()) }
    viewModel { (songId: String, transpose: Int, notation: Notation) ->
        StageViewModel(songId, transpose, notation, get(), get(), get())
    }
}
