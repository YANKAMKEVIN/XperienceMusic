package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

private val nameKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

/**
 * The name typed big and centred, as in recent onboarding flows, with a greeting that appears
 * as soon as there is a name.
 */
@Composable
fun NameFieldGreeting(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    greeting: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    // The keyboard is up as soon as the question shows.
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = nameKeyboard,
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            cursorBrush = BrandColors.gradient,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    field()
                }
            },
        )
        AnimatedVisibility(
            visible = value.isNotBlank(),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
        ) {
            Text(
                greeting,
                style = MaterialTheme.typography.titleMedium.copy(brush = BrandColors.horizontalGradient),
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
    }
}
