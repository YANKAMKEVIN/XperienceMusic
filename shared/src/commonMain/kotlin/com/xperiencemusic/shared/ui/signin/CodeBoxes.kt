package com.xperiencemusic.shared.ui.signin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

/**
 * One rounded box per digit. It is a single text field underneath, so typing, deleting, pasting
 * and the keyboard's code suggestion all work as usual; the boxes only draw it.
 */
@Composable
fun CodeBoxes(
    code: String,
    length: Int,
    onCodeChange: (String) -> Unit,
    isError: Boolean,
    enabled: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    // Checking the code: the boxes freeze and the keyboard gets out of the way.
    LaunchedEffect(enabled) { if (!enabled) keyboard?.hide() }
    // A refused code shakes the boxes, like a head saying no.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            for (offset in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(offset, tween(durationMillis = 45))
        }
    }
    BasicTextField(
        // The cursor always stays at the end: digits are typed and erased from the right.
        value = TextFieldValue(code, selection = TextRange(code.length)),
        onValueChange = { onCodeChange(it.text.filter(Char::isDigit).take(length)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .focusRequester(focus)
            .semantics { contentDescription = description },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
                repeat(length) { index ->
                    DigitBox(
                        digit = code.getOrNull(index),
                        isCurrent = enabled && index == code.length,
                        isError = isError && enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun DigitBox(digit: Char?, isCurrent: Boolean, isError: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme
    val border: Brush = when {
        isError -> SolidColor(colors.error)
        isCurrent -> BrandColors.gradient
        digit != null -> SolidColor(colors.primary.copy(alpha = 0.6f))
        else -> SolidColor(colors.outlineVariant)
    }
    // A small pop when a digit lands.
    val pop by animateFloatAsState(if (digit != null) 1f else 0.6f, label = "digit")
    Box(
        modifier
            .aspectRatio(0.82f)
            .background(if (isError) colors.error.copy(alpha = 0.14f) else colors.surfaceContainer, shape)
            .border(if (isCurrent || isError) 2.dp else 1.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                digit.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) colors.error else colors.onSurface,
                modifier = Modifier.scale(pop),
            )
        } else if (isCurrent) {
            // Where the next digit goes.
            Box(Modifier.size(width = 2.dp, height = 28.dp).background(BrandColors.gradient, RoundedCornerShape(1.dp)))
        }
    }
}
