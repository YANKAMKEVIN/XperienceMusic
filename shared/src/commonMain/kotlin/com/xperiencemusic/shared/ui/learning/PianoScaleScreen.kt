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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xperiencemusic.music.pitch.*
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.presentation.learning.*
import com.xperiencemusic.shared.ui.brand.GradientButton
import com.xperiencemusic.shared.ui.components.AppCard
import com.xperiencemusic.shared.ui.platform.rememberMicrophonePermission
import com.xperiencemusic.shared.ui.theme.AppTheme
import com.xperiencemusic.shared.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PianoScaleScreen(onBack: () -> Unit, vm: PianoScaleViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val permission = rememberMicrophonePermission()
    if (state.phase in listOf(PianoScalePhase.PREPARING, PianoScalePhase.COUNTDOWN, PianoScalePhase.RECORDING))
        com.xperiencemusic.shared.ui.platform.KeepScreenOn()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.activate() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.interrupt() }
    DisposableEffect(vm) { onDispose { vm.interrupt() } }
    PianoScaleScreenContent(state, playback.isPlaying, onBack = { vm.interrupt(); onBack() },
        actions = PianoScaleActions(
            start = { permission.request { if (it) vm.start() else vm.denied() } },
            stop = vm::stop, countdown = vm::beginCountdown, cancel = { vm.cancelPreparation() },
            example = vm::listenExample, listen = vm::listenTake, stopAudio = vm::stopPlayback,
            configure = vm::configure, select = vm::selectStep, passage = vm::listenPassage,
            expected = vm::listenExpected, focus = vm::focusStep))
}
class PianoScaleActions(
    val start: () -> Unit = {}, val stop: () -> Unit = {}, val countdown: () -> Unit = {}, val cancel: () -> Unit = {},
    val example: () -> Unit = {}, val listen: () -> Unit = {}, val stopAudio: () -> Unit = {},
    val configure: (PianoScalePractice) -> Unit = {}, val select: (Int) -> Unit = {}, val passage: (Int) -> Unit = {},
    val expected: (Int) -> Unit = {}, val focus: (Int) -> Unit = {},
)
private fun PianoScaleKey.label() = when (this) { PianoScaleKey.C -> "Do majeur"; PianoScaleKey.G -> "Sol majeur"; PianoScaleKey.F -> "Fa majeur" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianoScaleScreenContent(state: PianoScaleUiState, playing: Boolean = false, onBack: () -> Unit,
    actions: PianoScaleActions = PianoScaleActions()) {
    Scaffold(topBar = { TopAppBar(title = { Text("Mon atelier piano") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
    }, windowInsets = WindowInsets(0)) }, contentWindowInsets = WindowInsets(0)) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            AppCard {
                Text(if (state.practice.focusStart == null) "UNE GAMME · UNE MAIN" else "MON PASSAGE À TRAVAILLER",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(state.practice.key.label(), style = MaterialTheme.typography.headlineSmall)
                Text("${if (state.practice.descending) "Descente" else "Montée"} · ${state.practice.bpm} BPM · une note par pulsation", Modifier.padding(top = Spacing.s))
                Text("Relâche chaque touche avant la suivante. Joue une seule fois, sans pédale ni accompagnement. Do4 est le Do central.", Modifier.padding(top = Spacing.s))
                Text("Correction expérimentale de notes isolées : les accords, la pédale et le bruit peuvent tromper l’analyse. Le responsable garde la validation finale.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.s))
            }
            if (!state.allowed) Text("Connecte-toi avec ton profil piano pour utiliser cet exercice.")
            else {
                if (!state.busy) PracticeSettings(state, actions)
                if (state.report == null || state.busy) TargetNotes(state)
                when (state.phase) {
                    PianoScalePhase.PREPARING -> AppCard {
                        Text("1 · Vérifions le micro", style = MaterialTheme.typography.titleLarge)
                        Text("Joue une note pour vérifier le signal, puis relâche-la. Ce test n’est pas enregistré.", Modifier.padding(top = Spacing.s))
                        LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.m))
                        Text(when {
                            state.signal == PianoInputSignal.LOUD -> "Le son sature : éloigne un peu le téléphone ou baisse le volume."
                            state.signalSeen -> "Le micro reçoit du son. Attends qu’il s’éteigne avant de commencer."
                            else -> "J’attends un son. Rapproche le téléphone du piano si nécessaire."
                        }, Modifier.padding(top = Spacing.s).semantics { liveRegion = LiveRegionMode.Polite })
                        GradientButton(onClick = actions.countdown, enabled = state.signalSeen && state.signal != PianoInputSignal.LOUD,
                            modifier = Modifier.padding(top = Spacing.m)) { Text("Je suis prêt · démarrer dans 3 secondes") }
                        TextButton(onClick = actions.cancel) { Text("Annuler le test micro") }
                    }
                    PianoScalePhase.COUNTDOWN -> AppCard {
                        Text("2 · Prépare ta main", style = MaterialTheme.typography.titleLarge)
                        Text("${state.countdown}", style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(vertical = Spacing.l).semantics { liveRegion = LiveRegionMode.Assertive })
                        Text("Le signal de départ sera visuel. Aucun son de l’app ne sera joué pendant la prise.")
                        TextButton(onClick = actions.cancel) { Text("Annuler le départ") }
                    }
                    PianoScalePhase.RECORDING -> AppCard {
                        Text("À TOI", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Text("${state.seconds.toInt()} / ${PianoScaleViewModel.MAX_SECONDS} s · ${state.practice.bpm} BPM")
                        LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.m))
                        Text(if (state.visualBeat == null) "Tu peux arrêter après ta dernière note." else "Suis le repère visuel à ton rythme.", Modifier.padding(top = Spacing.s))
                        Text("Le repère est indicatif ; il ne mesure pas ta synchronisation exacte.", style = MaterialTheme.typography.bodySmall)
                        GradientButton(onClick = actions.stop, modifier = Modifier.padding(top = Spacing.m)) { Text("Arrêter et voir mon bilan") }
                    }
                    PianoScalePhase.ANALYZING -> {
                        CircularProgressIndicator()
                        Text("Je prépare ton retour sur ce téléphone…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    else -> {
                        state.report?.let { ScaleReport(state, it, actions) }
                        OutlinedButton(onClick = actions.example) { Text("Écouter l’exemple · ${state.practice.bpm} BPM") }
                        if (state.phase == PianoScalePhase.RESULT && state.seconds > 0)
                            OutlinedButton(onClick = actions.listen) { Text("Réécouter toute ma prise") }
                        GradientButton(onClick = actions.start) { Text(if (state.phase == PianoScalePhase.RESULT) "Refaire le même exercice" else "Préparer le micro") }
                    }
                }
            }
            if (playing) OutlinedButton(onClick = actions.stopAudio) { Text("Arrêter l’écoute") }
            state.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Text("Essai local, sans envoi au serveur. L’audio est effacé au nouvel essai ou au changement d’exercice, à la sortie ou en arrière-plan. Seul le bilan précédent reste temporairement pour comparer deux essais identiques ; il est aussi effacé à la sortie. Aucun niveau n’est validé automatiquement.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PracticeSettings(s: PianoScaleUiState, actions: PianoScaleActions) {
    AppCard {
        Text("Mon exercice", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PianoScaleKey.entries.forEach { key -> FilterChip(selected = s.practice.key == key,
                onClick = { actions.configure(s.practice.copy(key = key, focusStart = null)) }, label = { Text(key.label()) }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf(false, true).forEach { down -> FilterChip(selected = s.practice.descending == down,
                onClick = { actions.configure(s.practice.copy(descending = down, focusStart = null)) }, label = { Text(if (down) "Descente" else "Montée") }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf(40, 60, 80).forEach { bpm -> FilterChip(selected = s.practice.bpm == bpm,
                onClick = { actions.configure(s.practice.copy(bpm = bpm)) }, label = { Text("$bpm BPM") }) }
        }
        Text("Le tempo règle l’exemple et le guide visuel. La correction évalue les notes, sans imposer une synchronisation exacte.", style = MaterialTheme.typography.bodySmall)
        if (s.practice.focusStart != null) TextButton(onClick = { actions.configure(s.practice.copy(focusStart = null)) }) { Text("Revenir à la gamme complète") }
        if (s.report != null) Text("Changer d’exercice efface la prise et sa comparaison.", style = MaterialTheme.typography.bodySmall)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun TargetNotes(s: PianoScaleUiState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        s.practice.notes.forEachIndexed { index, note ->
            val active = s.phase == PianoScalePhase.RECORDING && s.visualBeat == index
            Surface(color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium, modifier = Modifier.semantics { stateDescription = if (active) "repère actuel" else "note attendue" }) {
                Text("${index + 1} · ${noteLabel(note)}", Modifier.padding(Spacing.m), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ScaleReport(s: PianoScaleUiState, report: PianoScaleReport, actions: PianoScaleActions) {
    AppCard {
        Text("3 · Ton bilan", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (report.quality != PianoScaleQuality.USABLE) {
            Text(when (report.quality) {
                PianoScaleQuality.TOO_SHORT -> "La prise est trop courte. Joue lentement tout l’exercice, puis arrête."
                PianoScaleQuality.TOO_QUIET -> "Le son est trop faible pour conclure. Rapproche le téléphone du piano."
                PianoScaleQuality.CLIPPED -> "Le son sature. Éloigne un peu le téléphone ou baisse le volume du piano."
                else -> "Je ne distingue pas assez clairement les notes. Recommence lentement, sans pédale, avec une seule main."
            }, modifier = Modifier.padding(top = Spacing.s))
            Text("Aucun verdict sur les notes et aucune comparaison pour cette prise.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("${report.matched} / ${report.expectedNotes.size} notes attendues retrouvées", Modifier.padding(top = Spacing.s))
            Text("Touche une note pour réécouter son passage. Les repères et la détection restent approximatifs.", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                report.steps.forEachIndexed { index, step ->
                    val status = when (step.correction) {
                        PianoNoteCorrection.MATCH -> "✓ repérée"
                        PianoNoteCorrection.WRONG_OCTAVE -> "octave"
                        PianoNoteCorrection.MISSING -> "non repérée"
                        PianoNoteCorrection.EXTRA -> "+ détectée"
                        else -> "à vérifier"
                    }
                    OutlinedButton(onClick = { actions.select(index); actions.passage(index) }, enabled = !s.preparingReplay,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (s.selectedStep == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                        modifier = Modifier.heightIn(min = 64.dp).semantics { selected = s.selectedStep == index; stateDescription = status }) {
                        Column { Text(step.expected?.let(::noteLabel) ?: step.heard?.let { noteLabel(it.midi) }.orEmpty()); Text(status, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            s.selectedStep?.let { index -> report.steps.getOrNull(index)?.let { step ->
                val expected = step.expected?.let(::noteLabel).orEmpty()
                val heard = step.heard?.let { noteLabel(it.midi) }.orEmpty()
                Text(when (step.correction) {
                    PianoNoteCorrection.MATCH -> "$expected a été repérée dans cette prise."
                    PianoNoteCorrection.WRONG_OCTAVE -> "$heard détectée au lieu de $expected : vérifie l’octave."
                    PianoNoteCorrection.WRONG_NOTE -> "$heard détectée au lieu de $expected : reprends cette transition."
                    PianoNoteCorrection.MISSING -> "$expected n’a pas été repérée. Elle peut avoir été omise ou mal détectée."
                    PianoNoteCorrection.EXTRA -> "$heard supplémentaire détectée : vérifie ce passage."
                }, Modifier.padding(top = Spacing.m).semantics { liveRegion = LiveRegionMode.Polite })
                step.heard?.let { Text("Repère ≈ ${(it.startSeconds * 10).toInt() / 10.0} s", style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { actions.passage(index) }, enabled = !s.preparingReplay) { Text(if (s.preparingReplay) "Préparation du passage…" else "Réécouter ce passage") }
                if (step.expected != null) TextButton(onClick = { actions.expected(index) }) { Text("Écouter la note attendue · $expected") }
                OutlinedButton(onClick = { actions.focus(index) }) { Text("Travailler autour de cette note · 3 notes") }
            } }
            report.irregularSpacing?.let { irregular -> Text(if (irregular)
                "L’espacement des notes varie : garde un mouvement lent et régulier."
                else "L’espacement des notes est régulier sur cette prise.", Modifier.padding(top = Spacing.m)) }
            s.comparison?.let { comparison ->
                HorizontalDivider(Modifier.padding(vertical = Spacing.m))
                Text("Depuis ton dernier essai exploitable", style = MaterialTheme.typography.titleMedium)
                Text("${comparison.previousMatched} → ${comparison.currentMatched} notes attendues retrouvées sur ${report.expectedNotes.size}.")
                if (comparison.regained.isNotEmpty()) Text("Cette fois, la détection retrouve : ${comparison.regained.joinToString(" · ", transform = ::noteLabel)}.")
                if (comparison.toCheckAgain.isNotEmpty()) Text("À vérifier de nouveau : ${comparison.toCheckAgain.joinToString(" · ", transform = ::noteLabel)}.")
                if (comparison.regained.isEmpty() && comparison.toCheckAgain.isEmpty()) Text("Les mêmes notes sont repérées. Consolide ce passage tranquillement.")
                Text("Même exercice et même tempo. Ce retour compare deux détections, sans valider une maîtrise définitive.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Preview @Composable private fun PianoWorkshopPreview() = AppTheme {
    PianoScaleScreenContent(PianoScaleUiState(allowed = true, phase = PianoScalePhase.PREPARING, signalSeen = true), onBack = {})
}
