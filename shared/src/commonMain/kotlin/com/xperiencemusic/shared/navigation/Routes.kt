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
