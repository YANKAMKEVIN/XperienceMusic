package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.music.theory.Notation

/** "Sol", "Lam": the key as it fits in a pill. */
fun Key.shortLabel(): String = tonicName(Notation.SOLFEGE) + if (mode == Mode.MINOR) "m" else ""
