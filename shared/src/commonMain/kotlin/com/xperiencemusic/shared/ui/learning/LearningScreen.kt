package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.presentation.learning.*
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.progress.ExerciseStatusBadge
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LearningScreen(onBack: () -> Unit, viewModel: LearningViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearningScreenContent(state, viewModel::onEvent, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreenContent(state: LearningUiState, onEvent: (LearningEvent) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Accords de base") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
        }, windowInsets = WindowInsets(0))
    }, contentWindowInsets = WindowInsets(0)) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !state.allowed -> Text("Ce premier module est proposé au parcours piano.", Modifier.padding(padding).padding(Spacing.l))
            else -> key(state.index) {
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    AppCard {
                        Text("PIANO · PARCOURS GUIDÉ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Construire, entendre, jouer", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = Spacing.s))
                        Text("${state.progress.completed.size} / ${PianoBasics.activities.size} activités terminées", modifier = Modifier.padding(top = Spacing.m))
                        LinearProgressIndicator(progress = { state.progress.completed.size.toFloat() / PianoBasics.activities.size }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.s))
                        Text("La compétence est évaluée séparément par ton responsable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.s))
                    }
                    if (state.progress.pending > 0) AppCard {
                        Text("${state.progress.pending} essai(s) sauvegardé(s) sur ce téléphone, en attente de synchronisation.")
                        TextButton(onClick = { onEvent(LearningEvent.Sync) }) { Text("Synchroniser") }
                    }
                    if (state.reviewDue) AppCard {
                        Text("À revoir aujourd’hui", style = MaterialTheme.typography.titleMedium)
                        Text("Retrouve Fa majeur de mémoire pour entretenir tes acquis.")
                        TextButton(onClick = { onEvent(LearningEvent.Open(PianoBasics.activities.lastIndex)) }) { Text("Reprendre le défi") }
                    }
                    ActivityCard(state, onEvent)
                    if (state.progress.isComplete) ReviewCard(state, onEvent)
                    Text("Les étapes du module", style = MaterialTheme.typography.titleLarge)
                    PianoBasics.activities.forEachIndexed { index, activity ->
                        val done = activity.id in state.progress.completed
                        val unlocked = state.progress.isComplete || index <= state.progress.nextIndex
                        AppCard(onClick = if (unlocked) ({ onEvent(LearningEvent.Open(index)) }) else null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                Text(if (done) "✓" else "${index + 1}", color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(Modifier.weight(1f)) {
                                    Text(activity.title, style = MaterialTheme.typography.titleSmall)
                                    Text(when { done -> "Terminée · à revoir librement"; !unlocked -> "À découvrir après l’étape précédente"; index == state.index -> "En cours"; else -> "À découvrir" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (state.progress.attempts.isNotEmpty()) AppCard {
                        Text("Mes derniers essais", style = MaterialTheme.typography.titleMedium)
                        state.progress.attempts.takeLast(5).reversed().forEach { attempt ->
                            val title = PianoBasics.activities.firstOrNull { it.id == attempt.activityId }?.title ?: "Activité"
                            Text("${attempt.day} · $title · ${if (attempt.passed) "terminé" else "à retravailler"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.s))
                        }
                    }
                    Text("Exemples sonores synthétisés. Les exercices à l’écran ne mesurent pas ton jeu sur l’instrument.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    val activity = state.activity
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text("ACTIVITÉ ${state.index + 1} · ${when (activity.kind) { LearningKind.LESSON -> "COMPRENDRE"; LearningKind.EAR -> "ÉCOUTER"; LearningKind.KEYBOARD -> "CONSTRUIRE"; LearningKind.PRACTICE -> "PRATIQUER" }}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(activity.title, style = MaterialTheme.typography.headlineSmall)
            Text(activity.instruction, style = MaterialTheme.typography.bodyLarge)
            if (activity.kind == LearningKind.LESSON || activity.kind == LearningKind.PRACTICE) Text(activity.explanation)
            if (activity.example.isNotEmpty()) OutlinedButton(onClick = { onEvent(LearningEvent.Listen) }, enabled = !state.saving) { Text("Écouter l’exemple") }
            if (activity.kind == LearningKind.EAR) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    listOf("Majeur", "Mineur").forEach { choice ->
                        FilterChip(selected = state.answer == choice, onClick = { onEvent(LearningEvent.Answer(choice)) }, label = { Text(choice) }, enabled = !state.saving)
                    }
                }
                if (!state.listened) Text("Écoute l’exemple avant de répondre.", style = MaterialTheme.typography.bodySmall)
            }
            if (activity.kind == LearningKind.KEYBOARD) {
                PianoKeyboard(state.notes, !state.saving) { onEvent(LearningEvent.Key(it)) }
                Text("Notes choisies : ${state.notes.sorted().joinToString(" · ") { noteName(it) }.ifEmpty { "aucune" }}", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { onEvent(LearningEvent.ListenSelection) }, enabled = state.notes.isNotEmpty()) { Text("Écouter ma sélection") }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.result?.let { passed ->
                Text(if (passed) "Bien joué !" else "Pas encore. Essaie à nouveau.", style = MaterialTheme.typography.titleMedium, color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                if (activity.kind == LearningKind.KEYBOARD || activity.kind == LearningKind.EAR) Text(activity.explanation)
            }
            if (state.result == true && state.index < PianoBasics.activities.lastIndex) {
                GradientButton(onClick = { onEvent(LearningEvent.Next) }) { Text("Continuer") }
            } else {
                GradientButton(onClick = { onEvent(LearningEvent.Check) }, enabled = state.canCheck) {
                    Text(when { state.saving -> "Sauvegarde…"; state.result == true -> "Refaire le défi"; activity.kind == LearningKind.LESSON -> "J’ai compris"; activity.kind == LearningKind.PRACTICE -> "J’ai effectué cet entraînement"; else -> "Vérifier ma réponse" })
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(state: LearningUiState, onEvent: (LearningEvent) -> Unit) {
    AppCard {
        Text("Passer de l’écran à l’instrument", style = MaterialTheme.typography.titleLarge)
        Text("Prépare I–IV–V–I en Do, Sol, Ré, Fa, La et Mi, avec renversements. Ton responsable pourra écouter et évaluer ton jeu en répétition.", modifier = Modifier.padding(top = Spacing.s))
        state.reviewStatus?.let { ExerciseStatusBadge(it, Modifier.padding(top = Spacing.s)) }
        state.feedback?.let { Text("Retour du responsable : $it", modifier = Modifier.padding(top = Spacing.s)) }
        if (!state.reviewLoaded) Text("Connecte-toi au réseau pour retrouver ton évaluation et envoyer une demande.", modifier = Modifier.padding(top = Spacing.s))
        when (state.reviewStatus) {
            ExerciseStatus.VALIDATED -> Text("Compétence acquise. Continue à la pratiquer.", modifier = Modifier.padding(top = Spacing.s))
            ExerciseStatus.SUBMITTED -> Text("Demande envoyée. Organise un temps d’évaluation avec ton responsable.", modifier = Modifier.padding(top = Spacing.s))
            else -> GradientButton(onClick = { onEvent(LearningEvent.RequestReview) }, enabled = !state.saving && state.reviewLoaded, modifier = Modifier.padding(top = Spacing.m)) {
                Text(if (state.reviewStatus == ExerciseStatus.TO_REWORK) "Demander une nouvelle évaluation" else "Demander une évaluation")
            }
        }
    }
}

private fun noteName(midi: Int): String = listOf("Do", "Do♯", "Ré", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "La♭", "La", "Si♭", "Si")[midi % 12] + (midi / 12 - 1)

@Composable
private fun PianoKeyboard(selected: Set<Int>, enabled: Boolean, onKey: (Int) -> Unit) {
    val whites = listOf(60, 62, 64, 65, 67, 69, 71, 72)
    val blacks = listOf(61 to 1, 63 to 2, 66 to 4, 68 to 5, 70 to 6)
    val accent = MaterialTheme.colorScheme.primary
    BoxWithConstraints(Modifier.fillMaxWidth().height(180.dp)) {
        val width = maxWidth / whites.size
        Row(Modifier.fillMaxWidth()) {
            whites.forEach { midi ->
                Box(Modifier.weight(1f).height(178.dp).background(if (midi in selected) accent else Color(0xFFF4F3FA), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .border(1.dp, Color(0xFF555164), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .toggleable(value = midi in selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onKey(midi) })
                    .semantics { contentDescription = noteName(midi) }, contentAlignment = Alignment.BottomCenter) {
                    Text(noteName(midi), color = Color(0xFF161320), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
        }
        blacks.forEach { (midi, boundary) ->
            Box(Modifier.offset(x = width * boundary - width * 0.32f).width(width * 0.64f).height(106.dp)
                .background(if (midi in selected) accent else Color(0xFF161320), RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                .toggleable(value = midi in selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onKey(midi) })
                .semantics { contentDescription = noteName(midi) }, contentAlignment = Alignment.BottomCenter) {
                Text(noteName(midi).dropLast(1), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Preview
@Composable
private fun LearningPreview() = AppTheme {
    LearningScreenContent(LearningUiState(loading = false, allowed = true, index = 1), {}, {})
}
