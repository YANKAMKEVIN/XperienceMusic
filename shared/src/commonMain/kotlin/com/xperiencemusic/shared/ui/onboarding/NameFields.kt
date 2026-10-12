package com.xperiencemusic.shared.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.brand.XmIcons
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

private val nameKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

/** A: a card with an icon, the label inside, and the brand gradient as border while typing. */
@Composable
fun NameFieldCard(value: String, onValueChange: (String) -> Unit, label: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val border: Brush = if (isFocused) BrandColors.gradient else SolidColor(colors.outlineVariant)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = nameKeyboard,
        keyboardActions = KeyboardActions(onNext = { onDone() }),
        textStyle = TextStyle(color = colors.onSurface, fontSize = 18.sp),
        cursorBrush = BrandColors.gradient,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.surfaceContainer, shape)
                    .border(if (isFocused) 1.5.dp else 1.dp, border, shape)
                    .padding(horizontal = Spacing.m, vertical = Spacing.m),
            ) {
                Box(
                    Modifier.size(40.dp).background(colors.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(XmIcons.Person, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = Spacing.m)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (isFocused) colors.primary else colors.onSurfaceVariant)
                    Box { field() }
                }
                if (value.isNotBlank()) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.tertiary)
            }
        },
    )
}

/** B: a large name on a line that lights up with the brand gradient while typing. */
@Composable
fun NameFieldLine(value: String, onValueChange: (String) -> Unit, label: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.16.em),
            color = colors.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interaction,
            keyboardOptions = nameKeyboard,
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = BrandColors.gradient,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s, bottom = Spacing.s),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (isFocused) 2.dp else 1.dp)
                .background(if (isFocused) BrandColors.horizontalGradient else SolidColor(colors.outline), RoundedCornerShape(1.dp)),
        )
    }
}

/**
 * C: the name typed big and centred, as in recent onboarding flows, with a greeting that appears
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
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = nameKeyboard,
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            cursorBrush = BrandColors.gradient,
            modifier = Modifier.fillMaxWidth(),
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
