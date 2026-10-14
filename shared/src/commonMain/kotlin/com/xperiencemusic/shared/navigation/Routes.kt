package com.xperiencemusic.shared.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WeekRoute

@Serializable
data object LibraryRoute

@Serializable
data object ProgressRoute

@Serializable
data object PlanningRoute

/** [monthStart] (ISO): the member's answers for that month. */
@Serializable
data class AvailabilityRoute(val monthStart: String)

/** [date] (ISO): the team of that Friday or Sunday. */
@Serializable
data class ServiceRoute(val date: String)

@Serializable
data class SongRoute(val songId: String)

/** [songId] null creates a new song. */
@Serializable
data class SongEditorRoute(val songId: String? = null)

/** [serviceDate] (ISO) edits that week's setlist; null prepares the next week without one. */
@Serializable
data class SetlistEditorRoute(val serviceDate: String? = null)

@Serializable
data class RecordingFormRoute(val songId: String)

@Serializable
data class ExerciseRoute(val exerciseId: String)

@Serializable
data class VoiceRecordRoute(val songId: String)

@Serializable
data object VoiceReviewRoute

/** [notation] is a [com.xperiencemusic.music.theory.Notation] name; routes only carry simple types. */
@Serializable
data class StageRoute(val songId: String, val transposeSemitones: Int, val notation: String)

@Serializable
data object MembersRoute
