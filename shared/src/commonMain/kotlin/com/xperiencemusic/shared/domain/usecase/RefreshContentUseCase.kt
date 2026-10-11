package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository

/** Pull-to-refresh: fetches songs and the setlist again. */
class RefreshContentUseCase(
    private val songs: SongRepository,
    private val setlists: SetlistRepository,
) {
    suspend operator fun invoke(): DomainResult<Unit> = runRemote {
        songs.refresh()
        setlists.refresh()
    }
}
