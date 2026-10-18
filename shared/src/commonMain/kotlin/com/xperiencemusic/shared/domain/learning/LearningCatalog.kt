package com.xperiencemusic.shared.domain.learning

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.abs

data class LearningModule(val id: String, val title: String, val objectiveId: String, val activities: List<LearningActivity>)

object LearningCatalog {
    const val DEFAULT = "piano-chords"
    val modules = listOf(
        LearningModule(DEFAULT, "Accords de base", "piano-c2", PianoBasics.activities),
        LearningModule("piano-scales", "Gammes et repères", "piano-c1", listOf(
            lesson("scale-understand", "Les pas de la gamme", "Une gamme majeure suit : ton, ton, demi-ton, ton, ton, ton, demi-ton.", "Un ton correspond à deux touches voisines successives ; un demi-ton à une seule. En Do : Do Ré Mi Fa Sol La Si Do."),
            keys("scale-half", "Trouver le demi-ton", "Sélectionne Mi4 et Fa4 : ces deux touches blanches sont voisines.", setOf(64,65), "Mi et Fa sont séparés d’un demi-ton, sans touche noire entre les deux."),
            keys("scale-triad", "Les repères de Do", "Sélectionne les degrés I, III et V de Do majeur.", setOf(60,64,67), "Compte Do(1), Ré(2), Mi(3), Fa(4), Sol(5)."),
            LearningActivity("scale-ear", "Écouter la montée", LearningKind.EAR, "Écoute les notes séparées : l’accord est-il majeur ou mineur ?", "Do–Mi–Sol forme une triade majeure issue de la gamme de Do.", listOf(60,64,67), answer="Majeur"),
            lesson("scale-fingering", "Travailler sans tension", "Sur l’instrument, commence par une main et un tempo confortable. Arrête en cas de douleur.", "Do majeur main droite en montant : 1–2–3–1–2–3–4–5. Main gauche : 5–4–3–2–1–3–2–1. Fais passer le pouce sans tordre le poignet ; demande au responsable de vérifier ta posture."),
            LearningActivity("scale-practice", "Deux mains, progressivement", LearningKind.PRACTICE, "Travaille Do, Sol, Ré, Fa, La et Mi majeur, mains séparées puis ensemble, sur une octave.", "Commence lentement. Le critère final est une octave à deux mains à 70 BPM dans ces six tonalités. Cette pratique est déclarative : le clavier virtuel ne vérifie ni ton doigté ni ta posture."),
        )),
        LearningModule("piano-rhythm", "Rythme et tempo", "piano-c3", listOf(
            lesson("rhythm-pulse", "Sentir la pulsation", "La pulsation est le battement régulier qui soutient la musique. À 60 BPM, il y a un battement par seconde.", "En 4/4, compte 1–2–3–4. Une noire dure un temps, deux croches partagent un temps."),
            LearningActivity("rhythm-count", "Partager un temps", LearningKind.CHOICE, "Combien de croches occupent la durée d’une noire ?", "Deux croches durent autant qu’une noire.", answer="2", choices=listOf("1","2","4")),
            LearningActivity("rhythm-tap", "Garder le tempo", LearningKind.RHYTHM, "Écoute le clic, puis frappe huit pulsations régulières à 72 BPM. Le premier appui sert de départ.", "L’exercice mesure les intervalles entre tes appuis, pas ton jeu au piano. Vise une pulsation stable, sans accélérer.", bpm=72),
            LearningActivity("rhythm-six", "Comprendre le 6/8", LearningKind.CHOICE, "Dans un 6/8 ressenti en deux grandes pulsations, combien de croches contient chaque grande pulsation ?", "Six croches sont groupées en deux ensembles de trois : 1–2–3 / 4–5–6.", answer="3", choices=listOf("2","3","6")),
            LearningActivity("rhythm-practice", "Trois motifs au métronome", LearningKind.PRACTICE, "Sur ton piano, garde le même tempo avec des accords plaqués, des arpèges puis un motif rythmique.", "Travaille chaque motif séparément puis enchaîne-les pendant trois minutes. La stabilité et l’accompagnement réel seront évalués par le responsable."),
        )),
        LearningModule("piano-chart", "Lire une grille", "piano-c4", listOf(
            lesson("chart-letters", "Deux écritures, un accord", "C = Do, D = Ré, E = Mi, F = Fa, G = Sol, A = La, B = Si. Le petit m indique mineur.", "Une grille indique les accords et la structure. Repère couplet, refrain et pont avant de jouer."),
            LearningActivity("chart-name", "Lire Am", LearningKind.CHOICE, "Quel accord est indiqué par Am ?", "A signifie La ; m signifie mineur : La mineur.", answer="La mineur", choices=listOf("La majeur","La mineur","Do mineur")),
            keys("chart-c", "Lire puis construire C", "La grille indique C. Construis cet accord en position fondamentale à partir de Do4.", setOf(60,64,67), "C signifie Do majeur : Do, Mi, Sol."),
            LearningActivity("chart-degree", "Lire les degrés", LearningKind.CHOICE, "En Do majeur, à quel accord correspond le degré IV ?", "Do est I ; Ré II ; Mi III ; Fa IV. Le degré IV est Fa majeur.", answer="Fa majeur", choices=listOf("Ré mineur","Fa majeur","Sol majeur")),
            LearningActivity("chart-practice", "Une première lecture", LearningKind.PRACTICE, "Sur ton instrument, lis C | Am | F | G : quatre temps par accord. Repère les changements avant de commencer.", "Puis demande au responsable une grille inconnue. Le critère final est une première lecture sans s’arrêter. La mémorisation de cet exemple ne suffit pas à valider la compétence."),
        )),
        LearningModule("piano-transpose", "Transposer", "piano-c5", listOf(
            lesson("transpose-rule", "Déplacer les mêmes relations", "Transposer consiste à déplacer toutes les notes du même intervalle. Un ton plus haut : ajoute deux demi-tons.", "C–F–G devient D–G–A. Les degrés I–IV–V restent identiques, mais la tonalité change."),
            LearningActivity("transpose-name", "Un ton plus haut", LearningKind.CHOICE, "Do majeur transposé un ton plus haut devient…", "Do → Do♯ → Ré : deux demi-tons donnent Ré majeur.", answer="Ré majeur", choices=listOf("Do♯ majeur","Ré majeur","Mi majeur")),
            keys("transpose-build", "Jouer le nouvel accord", "Transpose Do majeur un ton plus haut : construis Ré majeur à partir de Ré4.", setOf(62,66,69), "Déplace Do, Mi et Sol de deux demi-tons : Ré, Fa♯, La."),
            LearningActivity("transpose-grid", "Déplacer une grille", LearningKind.CHOICE, "C–F–G transposé un ton plus haut donne…", "Chaque accord monte d’un ton : D–G–A.", answer="D–G–A", choices=listOf("D–G–A","D–F–A","C–G–A")),
            LearningActivity("transpose-practice", "Changer de tonalité sur demande", LearningKind.PRACTICE, "Choisis un chant connu. Joue-le dans sa tonalité puis un ton plus haut et un ton plus bas.", "Utilise les degrés pour conserver les relations entre accords. Le responsable évaluera la transposition sur l’instrument, à la demande."),
        )),
    )
    fun module(id: String) = modules.firstOrNull { it.id == id } ?: modules.first()
    fun findActivity(id: String) = modules.flatMap { it.activities }.firstOrNull { it.id == id }
    private fun lesson(id:String,title:String,instruction:String,explanation:String) = LearningActivity(id,title,LearningKind.LESSON,instruction,explanation)
    private fun keys(id:String,title:String,instruction:String,notes:Set<Int>,explanation:String) = LearningActivity(id,title,LearningKind.KEYBOARD,instruction,explanation,expectedNotes=notes)
}

fun LearningProgress.completedIn(module: LearningModule) = module.activities.filter { activity ->
    attempts.any { it.activityId == activity.id && it.version == PianoBasics.VERSION && it.passed }
}.map { it.id }.toSet()
fun LearningProgress.nextIn(module: LearningModule) = module.activities.indexOfFirst { it.id !in completedIn(module) }.let { if(it<0) module.activities.lastIndex else it }
fun LearningProgress.complete(module: LearningModule) = completedIn(module).size == module.activities.size

/** Same-day repetitions do not artificially increase the spacing. Hint-assisted successes return sooner. */
fun LearningProgress.reviewDate(activityId:String): LocalDate? {
    val history=attempts.filter { it.activityId==activityId && it.version==PianoBasics.VERSION }
    val last=history.lastOrNull() ?: return null
    val streak=history.asReversed().takeWhile { it.passed && it.hints==0 }.map { it.day }.distinct().size
    val days=when { !last.passed || last.hints>0 -> 1; streak>=4 -> 30; streak==3 -> 14; streak==2 -> 7; else -> 3 }
    return last.day.plus(DatePeriod(days=days))
}
fun LearningProgress.dueIn(module:LearningModule,today:LocalDate) = module.activities.filter {
    it.kind !in listOf(LearningKind.LESSON,LearningKind.PRACTICE) && reviewDate(it.id)?.let { due -> due<=today }==true
}.sortedBy { reviewDate(it.id) }
fun LearningProgress.dailyPlan(module:LearningModule,today:LocalDate):List<Int> {
    val done=completedIn(module)
    val start=nextIn(module)
    val discovery=if(complete(module)) emptyList() else (start..minOf(start+2,module.activities.lastIndex)).toList()
    val revision=dueIn(module,today).map { module.activities.indexOf(it) }.take(1)
    return (revision+discovery).distinct().ifEmpty { listOf(module.activities.lastIndex) }
}

fun noteLabel(midi:Int)=listOf("Do","Do♯","Ré","Mi♭","Mi","Fa","Fa♯","Sol","La♭","La","Si♭","Si")[midi%12]+(midi/12-1)

/** The initial encounter stays unchanged; later encounters rotate tonic and quality deterministically. */
fun variedActivity(base:LearningActivity, encounter:Int):LearningActivity {
    if(encounter==0) return base
    if(base.kind==LearningKind.CHOICE) return base.copy(choices=if(encounter%2==0)base.choices else base.choices.reversed())
    if(base.kind==LearningKind.EAR) {
        val root=listOf(60,65,62,67,64,69)[encounter%6]
        val minor=(encounter+base.id.length)%2==0
        return base.copy(example=listOf(root,root+if(minor)3 else 4,root+7), answer=if(minor)"Mineur" else "Majeur",
            explanation="La tierce est à ${if(minor)3 else 4} demi-tons de la fondamentale : l’accord est ${if(minor)"mineur" else "majeur"}.")
    }
    if(base.kind==LearningKind.KEYBOARD && base.id.startsWith("piano-chords")) {
        val shift=listOf(0,2,5,7,9)[encounter%5]
        val notes=base.expectedNotes.map { it+shift }.toSet()
        val tonic=if(base.id==PianoBasics.REVIEW_ID)65 else 60
        val label=noteLabel(tonic+shift).dropLast(1)
        val quality=if(base.id.contains("minor"))"mineur" else "majeur"
        val position=when { base.id.contains("inversion")->"au premier renversement";base.id==PianoBasics.REVIEW_ID->"au deuxième renversement";else->"en position fondamentale" }
        return base.copy(title="À toi · $label $quality",instruction="Construis $label $quality $position. Pars du registre autour de ${noteLabel(notes.min())} et retrouve les intervalles.",
            expectedNotes=notes,explanation="Les intervalles restent identiques. Notes attendues : ${notes.sorted().joinToString(" · "){noteLabel(it)}}.")
    }
    return base
}

fun correction(activity:LearningActivity,notes:Set<Int>,answer:String?):String = when(activity.kind) {
    LearningKind.KEYBOARD -> {
        val correct=notes.intersect(activity.expectedNotes)
        val extra=notes-activity.expectedNotes
        val missing=activity.expectedNotes-notes
        val wrong=extra.firstOrNull(); val target=missing.firstOrNull { right -> wrong!=null && abs(right-wrong)<=1 }
        when {
            wrong!=null && target!=null -> "${correct.size} note(s) correcte(s). Tu as choisi ${noteLabel(wrong)} : déplace cette note vers ${noteLabel(target)}."
            wrong!=null && missing.any { it%12==wrong%12 } -> "Le nom est bon, mais vérifie l’octave de ${noteLabel(wrong)}."
            extra.isNotEmpty() -> "${correct.size} note(s) correcte(s). Retire ${extra.sorted().joinToString(" · "){noteLabel(it)}} puis vérifie les intervalles."
            else -> "${correct.size} note(s) correcte(s). Il manque ${missing.size} note(s) ; conserve ta sélection et cherche les suivantes."
        }
    }
    LearningKind.EAR -> "Tu as choisi ${answer.orEmpty()}. Réécoute la tierce ; utilise un indice avant de réessayer."
    else -> "Reprends la consigne ; un indice peut t’aider."
}
fun rhythmError(taps:List<Double>,bpm:Int):Double? {
    if(taps.size<8) return null
    val period=60.0/bpm
    return taps.zipWithNext { a,b -> abs((b-a)-period)/period }.average()
}
