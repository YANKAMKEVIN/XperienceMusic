package com.xperiencemusic.shared.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WeekRoute

@Serializable
data object LibraryRoute

@Serializable
data object ProgressRoute

@Serializable
data class SongRoute(val songId: String)

/** [songId] null creates a new song. */
@Serializable
data class SongEditorRoute(val songId: String? = null)

@Serializable
data object SetlistEditorRoute

@Serializable
data class RecordingFormRoute(val songId: String)

@Serializable
data class ExerciseRoute(val exerciseId: String)

/** [notation] is a [com.xperiencemusic.music.theory.Notation] name; routes only carry simple types. */
@Serializable
data class StageRoute(val songId: String, val transposeSemitones: Int, val notation: String)

@Serializable
data object MembersRoute
