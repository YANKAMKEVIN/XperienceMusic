package com.xperiencemusic.shared.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing

/** Full-screen explanation with one main action and a way out (used when not invited or offline). */
@Composable
fun SessionMessageScreen(
    title: String,
    text: String,
    primaryLabel: String?,
    onPrimary: () -> Unit,
    signOutLabel: String,
    onSignOut: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
            primaryLabel?.let {
                Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) { Text(it) }
            }
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(signOutLabel) }
        }
    }
}

@Preview
@Composable
private fun SessionMessageScreenPreview() {
    AppTheme {
        SessionMessageScreen(
            title = "Connexion impossible",
            text = "On n'arrive pas à joindre le serveur.",
            primaryLabel = "Réessayer",
            onPrimary = {},
            signOutLabel = "Se déconnecter",
            onSignOut = {},
        )
    }
}
