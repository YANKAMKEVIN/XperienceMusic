package com.xperiencemusic.shared.ui.learning

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.presentation.learning.ObjectiveTakeUi
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_take_label
import com.xperiencemusic.shared.resources.learning_take_local
import com.xperiencemusic.shared.resources.learning_take_pause
import com.xperiencemusic.shared.resources.learning_take_seconds
import com.xperiencemusic.shared.resources.learning_take_title
import org.jetbrains.compose.resources.stringResource

/**
 * The takes sent for an objective, newest first; a tap plays one or pauses it. [onPlay] gets the
 * labels the system player shows.
 */
@Composable
fun ObjectiveAudioReview(takes: List<ObjectiveTakeUi>, onPlay: (take: ObjectiveTakeUi, title: String, subtitle: String) -> Unit) {
    val title = stringResource(Res.string.learning_take_title)
    takes.forEach { take ->
        val subtitle = stringResource(Res.string.learning_take_seconds, take.seconds)
        OutlinedButton(onClick = { onPlay(take, title, subtitle) }) {
            Text(
                if (take.isPlaying) {
                    stringResource(Res.string.learning_take_pause)
                } else {
                    val day = take.day.ifEmpty { stringResource(Res.string.learning_take_local) }
                    stringResource(Res.string.learning_take_label, take.number, day, take.seconds)
                },
            )
        }
    }
}
