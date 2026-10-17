package com.xperiencemusic.shared.domain.learning

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/** Versioned activities train skills; only a responsible person validates the objective. */
enum class LearningKind { LESSON, EAR, KEYBOARD, PRACTICE }

data class LearningActivity(
    val id: String,
    val title: String,
    val kind: LearningKind,
    val instruction: String,
    val explanation: String,
    val example: List<Int> = emptyList(),
    val expectedNotes: Set<Int> = emptySet(),
    val answer: String? = null,
)

@Serializable
data class LearningAttempt(
    val id: String,
    val memberId: String,
    val activityId: String,
    val version: Int,
    val passed: Boolean,
    val day: LocalDate,
)

data class LearningProgress(val attempts: List<LearningAttempt> = emptyList(), val pending: Int = 0) {
    val completed: Set<String> get() = attempts.filter { it.passed && it.version == PianoBasics.VERSION && PianoBasics.activities.any { activity -> activity.id == it.activityId } }
        .map { it.activityId }.toSet()
    val nextIndex: Int get() = PianoBasics.activities.indexOfFirst { it.id !in completed }
        .let { if (it < 0) PianoBasics.activities.lastIndex else it }
    val isComplete: Boolean get() = PianoBasics.activities.all { it.id in completed }
    fun reviewDue(today: LocalDate): Boolean {
        val last = attempts.filter { it.passed && it.activityId == PianoBasics.REVIEW_ID && it.version == PianoBasics.VERSION }
            .maxOfOrNull { it.day } ?: return false
        return today >= last.plus(DatePeriod(days = 3))
    }
}

object PianoBasics {
    const val VERSION = 1
    const val OBJECTIVE_ID = "piano-c2"
    const val REVIEW_ID = "piano-chords-progression"
    val activities = listOf(
        LearningActivity("piano-chords-major", "Construire un accord majeur", LearningKind.LESSON,
            "Un accord de trois notes s’appelle une triade. Pour Do majeur, joue Do, Mi et Sol.",
            "Depuis Do : compte 4 demi-tons jusqu’à Mi, puis 3 jusqu’à Sol. Un demi-ton est le passage à la touche voisine, blanche ou noire. Formule : fondamentale, tierce majeure, quinte.", listOf(60, 64, 67)),
        LearningActivity("piano-chords-build-major", "À toi : Do majeur", LearningKind.KEYBOARD,
            "Sélectionne les trois notes de Do majeur sur le clavier, puis vérifie.",
            "Do majeur contient Do, Mi et Sol. Compte 4 demi-tons puis 3 ; aucune autre note n’est nécessaire.", expectedNotes = setOf(60, 64, 67)),
        LearningActivity("piano-chords-minor", "Passer au mineur", LearningKind.LESSON,
            "Pour passer de Do majeur à Do mineur, descends seulement la tierce d’un demi-ton : Mi devient Mi bémol.",
            "Do mineur : Do, Mi♭, Sol. Formule : 3 demi-tons puis 4. La fondamentale et la quinte restent les mêmes. Écoute les deux couleurs sans les réduire à une émotion unique.", listOf(60, 63, 67)),
        LearningActivity("piano-chords-ear-major", "Reconnaître la couleur · 1", LearningKind.EAR,
            "Écoute l’accord sans regarder ses notes. Est-il majeur ou mineur ? Tu peux le réécouter.",
            "C’est un accord majeur : sa tierce est à 4 demi-tons de la fondamentale.", listOf(62, 66, 69), answer = "Majeur"),
        LearningActivity("piano-chords-ear-minor", "Reconnaître la couleur · 2", LearningKind.EAR,
            "Même exercice, avec un autre accord. Écoute surtout la couleur de la tierce.",
            "C’est un accord mineur : sa tierce est à 3 demi-tons de la fondamentale.", listOf(64, 67, 71), answer = "Mineur"),
        LearningActivity("piano-chords-build-minor", "À toi : Do mineur", LearningKind.KEYBOARD,
            "Construis Do mineur. Une touche noire remplace le Mi de l’accord majeur.",
            "Les bonnes notes sont Do, Mi♭ et Sol. Mi♭ est la touche noire juste à gauche de Mi.", expectedNotes = setOf(60, 63, 67)),
        LearningActivity("piano-chords-inversions", "Changer l’ordre des notes", LearningKind.LESSON,
            "Do–Mi–Sol, Mi–Sol–Do et Sol–Do–Mi sont le même accord dans trois dispositions. Ce sont les renversements.",
            "Le premier renversement de Do majeur place Mi en bas : Mi4, Sol4, Do5. Garde les doigts près des touches et cherche un mouvement confortable, sans tension.", listOf(64, 67, 72)),
        LearningActivity("piano-chords-build-inversion", "À toi : premier renversement", LearningKind.KEYBOARD,
            "Place Mi4, Sol4 et Do5. Cette fois, le Do doit être la note la plus haute.",
            "Sélectionne Mi, Sol et le Do de droite (Do5). Le Do de gauche correspond à une autre disposition.", expectedNotes = setOf(64, 67, 72)),
        LearningActivity("piano-chords-practice", "Sur ton instrument · I–IV–V–I", LearningKind.PRACTICE,
            "En Do majeur, joue Do majeur → Fa majeur → Sol majeur → Do majeur. Commence lentement, puis essaie des renversements pour limiter les déplacements.",
            "Travaille ensuite en Sol, Ré, Fa, La et Mi. Compte quatre temps par accord. Si tu hésites, ralentis. Le critère final est d’enchaîner I–IV–V–I dans six tonalités sans hésiter, avec renversements. Cette étape d’entraînement reste déclarative ; le responsable évaluera ton jeu réel."),
        LearningActivity(REVIEW_ID, "Défi et révision · Fa majeur", LearningKind.KEYBOARD,
            "Sans revoir les explications, construis Fa majeur avec Do4 en bas. Tu réutilises les renversements.",
            "Fa majeur contient Fa, La et Do. Ici : Do4–Fa4–La4. Après ce défi, tu peux demander une évaluation sur ton instrument ; cela ne valide pas automatiquement la compétence.", expectedNotes = setOf(60, 65, 69)),
    )

    fun check(activity: LearningActivity, notes: Set<Int>, answer: String?): Boolean = when (activity.kind) {
        LearningKind.KEYBOARD -> notes == activity.expectedNotes
        LearningKind.EAR -> answer == activity.answer
        LearningKind.LESSON, LearningKind.PRACTICE -> true
    }
}
