package com.xperiencemusic.shared.ui.components

import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.instrument_bass
import com.xperiencemusic.shared.resources.instrument_drums
import com.xperiencemusic.shared.resources.instrument_piano
import com.xperiencemusic.shared.resources.instrument_singer
import com.xperiencemusic.shared.resources.level_beginner
import com.xperiencemusic.shared.resources.level_confirmed
import com.xperiencemusic.shared.resources.level_pro
import com.xperiencemusic.shared.resources.voice_alto
import com.xperiencemusic.shared.resources.voice_bass
import com.xperiencemusic.shared.resources.voice_soprano
import com.xperiencemusic.shared.resources.voice_tenor
import org.jetbrains.compose.resources.stringResource

@Composable
fun Instrument.label(): String = stringResource(
    when (this) {
        Instrument.SINGER -> Res.string.instrument_singer
        Instrument.PIANO -> Res.string.instrument_piano
        Instrument.BASS -> Res.string.instrument_bass
        Instrument.DRUMS -> Res.string.instrument_drums
    },
)

@Composable
fun VoicePart.label(): String = stringResource(
    when (this) {
        VoicePart.SOPRANO -> Res.string.voice_soprano
        VoicePart.ALTO -> Res.string.voice_alto
        VoicePart.TENOR -> Res.string.voice_tenor
        VoicePart.BASS -> Res.string.voice_bass
    },
)

@Composable
fun Level.label(): String = stringResource(
    when (this) {
        Level.BEGINNER -> Res.string.level_beginner
        Level.CONFIRMED -> Res.string.level_confirmed
        Level.PRO -> Res.string.level_pro
    },
)
