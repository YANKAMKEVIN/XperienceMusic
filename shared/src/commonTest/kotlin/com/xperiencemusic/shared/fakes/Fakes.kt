package com.xperiencemusic.shared.fakes

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemorySetlistRepository
import com.xperiencemusic.shared.data.repository.InMemorySongRepository
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

class FakeSessionRepository(member: Member? = null) : SessionRepository {
    val state = MutableStateFlow<SessionState>(member?.let { SessionState.Ready(it) } ?: SessionState.SignedOut)
    var failure: Exception? = null
    val requestedCodes = mutableListOf<String>()

    override fun observeSession(): Flow<SessionState> = state

    override suspend fun currentMember(): Member? = (state.value as? SessionState.Ready)?.member

    override suspend fun requestCode(email: String) {
        failure?.let { throw it }
        requestedCodes += email
    }

    override suspend fun verifyCode(email: String, code: String) {
        failure?.let { throw it }
        if (code != "123456") throw DomainException(DomainError.INVALID_CODE)
        state.value = SessionState.NeedsProfile(email)
    }

    override suspend fun saveProfile(profile: MemberProfile): Member {
        failure?.let { throw it }
        return Member("me", profile, currentMember()?.role ?: Role.MEMBER).also { state.value = SessionState.Ready(it) }
    }

    override suspend fun signOut() {
        failure?.let { throw it }
        state.value = SessionState.SignedOut
    }

    override suspend fun refresh() = Unit
}

fun member(
    role: Role = Role.MEMBER,
    instrument: Instrument = Instrument.SINGER,
    voicePart: VoicePart? = VoicePart.ALTO,
    level: Level = Level.BEGINNER,
) = Member("me", MemberProfile("Grâce", instrument, voicePart, level), role)

fun recording(id: String, day: Int, isReference: Boolean = false) =
    Recording(id, "Version $id", LocalDate(2026, 1, day), 200, "demo://$id", isReference)

fun song(
    id: String = "s1",
    title: String = "Chant",
    artist: String? = null,
    recordings: List<Recording> = emptyList(),
    chart: String = "[1]Gloire",
    key: Key = Key(0),
) = Song(id, title, artist, key, chart, recordings, emptyList(), emptyList())

fun songRepository(vararg songs: Song) = InMemorySongRepository(songs.toList())

fun setlistRepository() = InMemorySetlistRepository()

/** The setlists as the screens see them, on [today]. */
fun schedule(
    setlists: SetlistRepository,
    songs: SongRepository,
    today: LocalDate = LocalDate(2026, 10, 10),
) = ObserveSetlistScheduleUseCase(setlists, songs) { today }

fun exerciseRepository(vararg exercises: com.xperiencemusic.shared.domain.model.Exercise) =
    InMemoryExerciseRepository(exercises.toList())
