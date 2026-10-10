package com.xperiencemusic.shared.fakes

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemorySetlistRepository
import com.xperiencemusic.shared.data.repository.InMemorySongRepository
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

class FakeSessionRepository(member: Member? = null) : SessionRepository {
    val member = MutableStateFlow(member)

    override fun observeCurrentMember(): Flow<Member?> = member

    override suspend fun currentMember(): Member? = member.value

    override suspend fun saveProfile(profile: MemberProfile): Member =
        Member("me", profile, member.value?.role ?: Role.MEMBER).also { member.value = it }
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
) = Song(id, title, artist, Key(0), chart, recordings, emptyList(), emptyList())

fun songRepository(vararg songs: Song) = InMemorySongRepository(songs.toList())

fun setlistRepository() = InMemorySetlistRepository(null)

fun exerciseRepository(vararg exercises: com.xperiencemusic.shared.domain.model.Exercise) =
    InMemoryExerciseRepository(exercises.toList())
