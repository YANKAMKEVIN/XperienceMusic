package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.presentation.learning.LearningEvent
import com.xperiencemusic.shared.presentation.learning.LearningUiState
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_quiz_chord
import com.xperiencemusic.shared.resources.learning_quiz_chord_right
import com.xperiencemusic.shared.resources.learning_quiz_chord_wrong
import com.xperiencemusic.shared.resources.learning_quiz_from_zero
import com.xperiencemusic.shared.resources.learning_quiz_keep
import com.xperiencemusic.shared.resources.learning_quiz_note
import com.xperiencemusic.shared.resources.learning_quiz_result
import com.xperiencemusic.shared.resources.learning_quiz_rhythm
import com.xperiencemusic.shared.resources.learning_quiz_rhythm_right
import com.xperiencemusic.shared.resources.learning_quiz_rhythm_wrong
import com.xperiencemusic.shared.resources.learning_quiz_title
import com.xperiencemusic.shared.resources.learning_quiz_transpose
import com.xperiencemusic.shared.resources.learning_quiz_transpose_right
import com.xperiencemusic.shared.resources.learning_quiz_transpose_wrong
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A starting-point question: the answers in order, and which one is right. */
private class QuizQuestion(val question: StringResource, val answers: List<StringResource>, val right: Int)

private val QUIZ = listOf(
    QuizQuestion(Res.string.learning_quiz_chord, listOf(Res.string.learning_quiz_chord_right, Res.string.learning_quiz_chord_wrong), right = 0),
    QuizQuestion(Res.string.learning_quiz_rhythm, listOf(Res.string.learning_quiz_rhythm_wrong, Res.string.learning_quiz_rhythm_right), right = 1),
    QuizQuestion(
        Res.string.learning_quiz_transpose,
        listOf(Res.string.learning_quiz_transpose_right, Res.string.learning_quiz_transpose_wrong),
        right = 0,
    ),
)

/** Three optional questions that suggest a piano module to start from; nothing is validated or skipped. */
@Composable
internal fun StartingPoint(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    var stage by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    AppCard {
        Text(stringResource(Res.string.learning_quiz_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.learning_quiz_note), style = MaterialTheme.typography.bodySmall)
        if (stage < QUIZ.size) {
            val question = QUIZ[stage]
            Text(stringResource(question.question), Modifier.padding(top = Spacing.s))
            question.answers.forEachIndexed { index, answer ->
                OutlinedButton(onClick = {
                    if (index == question.right) score++
                    stage++
                }) {
                    Text(stringResource(answer))
                }
            }
        } else {
            val recommendation = when {
                score == 0 -> LearningCatalog.DEFAULT
                score < QUIZ.size -> "piano-rhythm"
                else -> "piano-chart"
            }
            Text(stringResource(Res.string.learning_quiz_result, score, LearningCatalog.module(recommendation).title))
            TextButton(onClick = { onEvent(LearningEvent.Preferences(state.preferences.copy(startingPoint = recommendation))) }) {
                Text(stringResource(Res.string.learning_quiz_keep))
            }
        }
        TextButton(onClick = { onEvent(LearningEvent.Preferences(state.preferences.copy(startingPoint = LearningCatalog.DEFAULT))) }) {
            Text(stringResource(Res.string.learning_quiz_from_zero))
        }
    }
}
