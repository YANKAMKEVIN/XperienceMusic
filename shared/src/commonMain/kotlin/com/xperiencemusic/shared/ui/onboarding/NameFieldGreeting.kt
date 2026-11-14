package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

private val nameKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)

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
    val focusManager = LocalFocusManager.current
    // Centred while empty, the cursor sat in the middle of "Ton prénom": it moves to the hint's start.
    var placeholderWidth by remember { mutableIntStateOf(0) }
    val fontSize = nameFontSize(value.length)
    // The keyboard is up as soon as the question shows.
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = nameKeyboard,
            // "OK" closes the keyboard without leaving a focus that looks like a choice on the next question.
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onDone() }),
            textStyle = TextStyle(color = colors.onSurface, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            cursorBrush = BrandColors.gradient,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center,
                            onTextLayout = { placeholderWidth = it.size.width },
                        )
                    }
                    Box(Modifier.offset { IntOffset(if (value.isEmpty()) -placeholderWidth / 2 else 0, 0) }) { field() }
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

/** Big for a short name, smaller as it grows, so a long first name stays whole on one line. */
internal fun nameFontSize(length: Int): TextUnit =
    NAME_FONT_SIZES.firstOrNull { (maxLength, _) -> length <= maxLength }?.second ?: NAME_FONT_SIZE_LONGEST

/** Up to so many letters, this size. Local to the field: no other text adapts its size this way. */
private val NAME_FONT_SIZES = listOf(10 to 34.sp, 14 to 28.sp, 18 to 24.sp, 24 to 20.sp)
private val NAME_FONT_SIZE_LONGEST = 16.sp
