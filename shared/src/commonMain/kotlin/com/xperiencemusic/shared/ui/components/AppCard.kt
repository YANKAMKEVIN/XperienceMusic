package com.xperiencemusic.shared.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xperiencemusic.shared.ui.theme.Radius
import com.xperiencemusic.shared.ui.theme.Spacing

/** Raised surface used for list rows and grouped content. Clickable when [onClick] is set. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Radius.card)
    val color = MaterialTheme.colorScheme.surfaceContainer
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(Spacing.l), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier.fillMaxWidth(), content = inner)
    } else {
        Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth(), content = inner)
    }
}
