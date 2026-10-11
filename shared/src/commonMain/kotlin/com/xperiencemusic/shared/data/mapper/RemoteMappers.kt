package com.xperiencemusic.shared.data.mapper

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.shared.data.remote.dto.InvitationDto
import com.xperiencemusic.shared.data.remote.dto.ProfileDto
import com.xperiencemusic.shared.data.remote.dto.ProfileUpdateDto
import com.xperiencemusic.shared.data.remote.dto.RecordingDto
import com.xperiencemusic.shared.data.remote.dto.SetlistDto
import com.xperiencemusic.shared.data.remote.dto.SongDto
import com.xperiencemusic.shared.data.remote.dto.SongWriteDto
import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Invitation
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.model.SongLink
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceTrack

/** Null while the member has not answered the onboarding questions. */
fun ProfileDto.toDomain(): Member? {
    val name = displayName ?: return null
    val instrument = enumOrNull<Instrument>(instrument) ?: return null
    val level = enumOrNull<Level>(level) ?: return null
    return Member(
        id = id,
        profile = MemberProfile(name, instrument, enumOrNull<VoicePart>(voicePart), level),
        // An unknown role from a newer server must never grant more than reading.
        role = enumOrNull<Role>(role) ?: Role.MEMBER,
    )
}

fun ProfileDto.toBandMember() = BandMember(
    id = id,
    email = email,
    displayName = displayName,
    role = enumOrNull<Role>(role) ?: Role.MEMBER,
    instrument = enumOrNull<Instrument>(instrument),
    voicePart = enumOrNull<VoicePart>(voicePart),
)

fun InvitationDto.toDomain() = Invitation(email = email, role = enumOrNull<Role>(role) ?: Role.MEMBER)

fun MemberProfile.toUpdateDto() = ProfileUpdateDto(
    displayName = displayName,
    instrument = instrument.name,
    voicePart = voicePart?.name,
    level = level.name,
)

fun SongDto.toDomain() = Song(
    id = id,
    title = title,
    artist = artist,
    key = Key(keyTonic, if (keyMode == Mode.MINOR.name) Mode.MINOR else Mode.MAJOR),
    chart = chart,
    recordings = recordings.map { it.toDomain() },
    voiceTracks = voiceTracks.mapNotNull { track ->
        enumOrNull<VoicePart>(track.voicePart)?.let { VoiceTrack(track.id, it, track.storagePath) }
    },
    links = links.sortedBy { it.position }.map { SongLink(it.label, it.url) },
)

fun SongDraft.toWriteDto() = SongWriteDto(
    title = title,
    artist = artist,
    keyTonic = key.tonic,
    keyMode = key.mode.name,
    chart = chart,
)

fun RecordingDto.toDomain() = Recording(
    id = id,
    title = title,
    recordedOn = recordedOn,
    durationSeconds = durationSeconds,
    audioPath = storagePath,
    isReference = isReference,
)

fun SetlistDto.toDomain() = Setlist(
    id = id,
    rehearsalDate = rehearsalDate,
    serviceDate = serviceDate,
    songIds = songs.sortedBy { it.position }.map { it.songId },
    message = message,
    publishedBy = publishedByName,
)

fun String.toExerciseStatus(): ExerciseStatus = enumOrNull<ExerciseStatus>(this) ?: ExerciseStatus.TODO

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
