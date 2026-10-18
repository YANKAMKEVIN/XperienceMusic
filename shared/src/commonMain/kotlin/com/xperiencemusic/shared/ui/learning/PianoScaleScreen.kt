package com.xperiencemusic.shared.ui.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.pitch.*
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.presentation.learning.*
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PianoScaleScreen(onBack: () -> Unit, vm: PianoScaleViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val permission = rememberMicrophonePermission()
    if (state.phase == PianoScalePhase.RECORDING) com.xperiencemusic.shared.ui.platform.KeepScreenOn()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.activate() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.interrupt() }
    DisposableEffect(vm) { onDispose { vm.interrupt() } }
    PianoScaleScreenContent(state, onBack = { vm.interrupt(); onBack() },
        onStart = { permission.request { if (it) vm.start() else vm.denied() } },
        onStop = vm::stop, onExample = vm::listenExample, onListen = vm::listenTake)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianoScaleScreenContent(state: PianoScaleUiState, onBack: () -> Unit, onStart: () -> Unit,
    onStop: () -> Unit, onExample: () -> Unit, onListen: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Corriger ma gamme") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
    }, windowInsets = WindowInsets(0)) }, contentWindowInsets = WindowInsets(0)) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            AppCard {
                Text("DO MAJEUR · UNE MAIN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Écouter, jouer, comprendre", style = MaterialTheme.typography.headlineSmall)
                Text("Do4 · Ré4 · Mi4 · Fa4 · Sol4 · La4 · Si4 · Do5", Modifier.padding(top = Spacing.s))
                Text("Joue une seule montée, environ une note par seconde. Relâche chaque touche avant la suivante, sans pédale ni accompagnement. Do4 est le Do central.", Modifier.padding(top = Spacing.s))
                Text("Pose le téléphone près du piano dans une pièce calme. Écoute l’exemple avant d’enregistrer, puis attends la fin du son.", Modifier.padding(top = Spacing.s))
                Text("Correction expérimentale de notes isolées : les accords et la pédale peuvent tromper l’analyse. Le responsable garde la validation de la compétence.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.s))
            }
            if (!state.allowed) Text("Connecte-toi avec ton profil piano pour utiliser cet exercice.")
            else when (state.phase) {
                PianoScalePhase.RECORDING -> {
                    Text("Enregistrement · ${state.seconds.toInt()} / ${PianoScaleViewModel.MAX_SECONDS} s", style = MaterialTheme.typography.titleLarge)
                    LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth())
                    Text("Joue la gamme une seule fois, puis arrête la prise.")
                    GradientButton(onClick = onStop) { Text("Arrêter et corriger") }
                }
                PianoScalePhase.ANALYZING -> {
                    CircularProgressIndicator()
                    Text("Analyse sur ton téléphone…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                else -> {
                    state.report?.let { ScaleReport(it) }
                    OutlinedButton(onClick = onExample) { Text("Écouter la gamme") }
                    if (state.phase == PianoScalePhase.RESULT) OutlinedButton(onClick = onListen) { Text("Réécouter ma prise") }
                    GradientButton(onClick = onStart) { Text(if (state.phase == PianoScalePhase.RESULT) "Faire un nouvel essai" else "Enregistrer ma gamme") }
                }
            }
            state.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Text("Essai local, sans envoi au serveur. La prise est effacée au nouvel essai, à la sortie de cet écran ou au passage en arrière-plan. Aucun niveau n’est validé automatiquement.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ScaleReport(report: PianoScaleReport) {
    AppCard {
        Text("Ton retour", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (report.quality != PianoScaleQuality.USABLE) {
            Text(when (report.quality) {
                PianoScaleQuality.TOO_SHORT -> "La prise est trop courte. Joue la montée complète, lentement, puis arrête."
                PianoScaleQuality.TOO_QUIET -> "Le son est trop faible pour conclure. Rapproche le téléphone du piano, puis recommence."
                PianoScaleQuality.CLIPPED -> "Le son sature. Éloigne un peu le téléphone ou baisse le volume du piano."
                else -> "Je ne distingue pas assez clairement les notes. Recommence plus lentement, sans pédale, avec une seule main et sans bruit de fond."
            }, modifier = Modifier.padding(top = Spacing.s))
            Text("Aucun verdict sur les notes de cette prise.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("${report.matched} / ${PianoScaleExercise.notes.size} notes attendues retrouvées", Modifier.padding(top = Spacing.s))
            Text("Les repères temporels sont approximatifs. Une note non repérée peut aussi venir d’une détection imparfaite.", style = MaterialTheme.typography.bodySmall)
            report.steps.forEach { step ->
                val time = step.heard?.startSeconds?.let { "≈ ${(it * 10).toInt() / 10.0} s · " }.orEmpty()
                val expected = step.expected?.let(::noteLabel).orEmpty()
                val heard = step.heard?.let { noteLabel(it.midi) }.orEmpty()
                Text(time + when (step.correction) {
                    PianoNoteCorrection.MATCH -> "✓ $expected repérée"
                    PianoNoteCorrection.WRONG_OCTAVE -> "$heard détectée au lieu de $expected : vérifie l’octave."
                    PianoNoteCorrection.WRONG_NOTE -> "$heard détectée au lieu de $expected : reprends ce passage."
                    PianoNoteCorrection.MISSING -> "$expected non repérée : rejoue-la lentement entre ses voisines."
                    PianoNoteCorrection.EXTRA -> "$heard supplémentaire détectée : vérifie la transition."
                }, modifier = Modifier.padding(top = Spacing.s))
            }
            if (report.irregularSpacing != null) Text(if (report.irregularSpacing)
                "L’espacement des notes varie. Rejoue lentement en comptant régulièrement."
                else "L’espacement des notes est régulier sur cette prise.", Modifier.padding(top = Spacing.m))
            val first = report.steps.firstOrNull { it.correction != PianoNoteCorrection.MATCH }
            Text(if (first == null) "Prochain essai : retrouve cette montée avec la même régularité, sans tension."
                else "Prochain essai : isole la transition autour de ${first.expected?.let(::noteLabel) ?: first.heard?.let { noteLabel(it.midi) }} avant de refaire la montée.",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.m))
        }
    }
}
