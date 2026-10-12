package com.xperiencemusic.shared.ui.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xperiencemusic.shared.ui.theme.BrandColors
import com.xperiencemusic.shared.ui.theme.Spacing

private val emailKeyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send)

/** A pill with the send button inside it, like a message box: type, tap the arrow. */
@Composable
fun EmailFieldPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    sendLabel: String,
    isError: Boolean,
    canSend: Boolean,
    isSending: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = emailKeyboard,
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        textStyle = TextStyle(color = colors.onSurface, fontSize = 17.sp),
        cursorBrush = BrandColors.gradient,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colors.surfaceContainerHigh, CircleShape)
                    .border(
                        1.dp,
                        when {
                            isError -> SolidColor(colors.error)
                            isFocused -> BrandColors.gradient
                            else -> SolidColor(colors.outlineVariant)
                        },
                        CircleShape,
                    )
                    .padding(start = Spacing.l, end = 6.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 17.sp, color = colors.onSurfaceVariant)
                    field()
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend || isSending) BrandColors.gradient else SolidColor(colors.surfaceContainer))
                        .clickable(enabled = canSend, onClickLabel = sendLabel, role = Role.Button, onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = sendLabel,
                            tint = if (canSend) Color.White else colors.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}
