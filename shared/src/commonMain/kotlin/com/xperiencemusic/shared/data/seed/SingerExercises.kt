package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseCheck.HoldNote
import com.xperiencemusic.shared.domain.model.ExerciseCheck.NoteSequence

/**
 * The singers' path: five tiers checked by the microphone, each ending with a challenge, plus the
 * daily warm-up. Notes are semitones from the singer's comfort note, so every voice gets its own.
 */
internal object SingerExercises {
    private const val SCALE_UP = "0,2,4,5,7,9,11,12"

    private fun steps(text: String) = text.split(',').map { it.trim().toInt() }

    val all: List<Exercise> = listOf(
        Exercise(
            "warmup", Exercise.WARM_UP_TIER, 1, "Échauffement du jour",
            "Monte et redescends doucement cinq notes sur « mi », une seconde chacune. Une minute pour réveiller la voix et garder ta série.",
            NoteSequence(steps("0,2,4,2,0"), secondsEach = 1.0),
        ),

        // Tier 1 · Souffle et tenue
        Exercise(
            "t1-hold-5", 1, 1, "Tenir 5 secondes",
            "Inspire par le ventre, puis chante ta note de confort sur « a » et tiens-la sans bouger.",
            HoldNote(0, seconds = 5.0),
        ),
        Exercise(
            "t1-hold-10", 1, 2, "Tenir 10 secondes",
            "Même note, deux fois plus longtemps. Dose ton air dès le début pour ne pas faiblir à la fin.",
            HoldNote(0, seconds = 10.0),
        ),
        Exercise(
            "t1-breath-15", 1, 3, "Souffle long 15 s",
            "Une grande inspiration, puis tiens la note 15 secondes, avec un son régulier du début à la fin.",
            HoldNote(0, seconds = 15.0),
        ),
        Exercise(
            "t1-challenge", 1, 4, "Défi · l'escalier",
            "Trois marches : tiens chaque note 4 secondes, puis monte d'un ton sans reprendre ton souffle si tu peux.",
            NoteSequence(steps("0,2,4"), secondsEach = 4.0),
            isChallenge = true,
        ),

        // Tier 2 · Justesse
        Exercise(
            "t2-heard", 2, 1, "La note entendue",
            "Écoute la note, puis chante-la et tiens-la 4 secondes. L'aiguille t'aide à te placer.",
            HoldNote(2, seconds = 4.0),
        ),
        Exercise(
            "t2-fifth", 2, 2, "Monter à la quinte",
            "Cinq notes au-dessus de ta note de confort. Écoute-la d'abord, puis tiens-la 5 secondes.",
            HoldNote(7, seconds = 5.0),
        ),
        Exercise(
            "t2-octave", 2, 3, "L'octave",
            "La même note, une octave plus haut. Ouvre la bouche, garde la gorge détendue, tiens 3 secondes.",
            HoldNote(12, seconds = 3.0),
        ),
        Exercise(
            "t2-challenge", 2, 4, "Défi · le grand saut",
            "Ta note, la quinte, puis l'octave : deux secondes chacune, bien justes.",
            NoteSequence(steps("0,7,12"), secondsEach = 2.0),
            isChallenge = true,
        ),

        // Tier 3 · Intervalles
        Exercise(
            "t3-third", 3, 1, "La tierce",
            "Ta note puis la tierce au-dessus, comme le début de « Do-Mi ». Tiens chacune une seconde et demie.",
            NoteSequence(steps("0,4"), secondsEach = 1.5),
        ),
        Exercise(
            "t3-fourth", 3, 2, "La quarte",
            "Ta note puis la quarte, l'intervalle des débuts d'hymnes. Une seconde et demie chacune.",
            NoteSequence(steps("0,5"), secondsEach = 1.5),
        ),
        Exercise(
            "t3-fifth", 3, 3, "La quinte enchaînée",
            "Ta note puis la quinte, sans t'arrêter entre les deux.",
            NoteSequence(steps("0,7"), secondsEach = 1.5),
        ),
        Exercise(
            "t3-challenge", 3, 4, "Défi · l'arpège",
            "Ta note, la tierce, la quinte, puis redescends : cinq notes enchaînées.",
            NoteSequence(steps("0,4,7,4,0"), secondsEach = 1.2),
            isChallenge = true,
        ),

        // Tier 4 · Gammes
        Exercise(
            "t4-five-up", 4, 1, "Cinq notes",
            "Monte les cinq premières notes de la gamme, une seconde chacune.",
            NoteSequence(steps("0,2,4,5,7"), secondsEach = 1.0),
        ),
        Exercise(
            "t4-five-updown", 4, 2, "Aller et retour",
            "Les cinq notes en montant, puis en redescendant.",
            NoteSequence(steps("0,2,4,5,7,5,4,2,0"), secondsEach = 0.8),
        ),
        Exercise(
            "t4-scale", 4, 3, "La gamme complète",
            "Huit notes jusqu'à l'octave, régulières.",
            NoteSequence(steps(SCALE_UP), secondsEach = 0.8),
        ),
        Exercise(
            "t4-challenge", 4, 4, "Défi · la gamme entière",
            "Toute la gamme en montant puis en descendant, sans te presser.",
            NoteSequence(steps("$SCALE_UP,11,9,7,5,4,2,0"), secondsEach = 0.7),
            isChallenge = true,
        ),

        // Tier 5 · Harmonie (with earphones: the other voice plays while you sing)
        Exercise(
            "t5-third-above", 5, 1, "Une voix au-dessus",
            "Mets des écouteurs. Une autre voix chante la tierce au-dessus : garde ta note 6 secondes sans te laisser emporter.",
            HoldNote(0, seconds = 6.0, accompanimentSemitones = 4),
        ),
        Exercise(
            "t5-fifth-above", 5, 2, "La quinte à côté",
            "Avec écouteurs. L'autre voix tient la quinte : reste sur ta note 6 secondes.",
            HoldNote(0, seconds = 6.0, accompanimentSemitones = 7),
        ),
        Exercise(
            "t5-under", 5, 3, "Chanter au-dessus",
            "Avec écouteurs. Cette fois l'autre voix est en dessous : tiens la tierce au-dessus d'elle 6 secondes.",
            HoldNote(4, seconds = 6.0, accompanimentSemitones = 0),
        ),
        Exercise(
            "t5-challenge", 5, 4, "Défi · tenir sa partie",
            "Avec écouteurs. L'autre voix tient la quinte pendant que tu gardes ta note 10 secondes, comme en répétition.",
            HoldNote(0, seconds = 10.0, accompanimentSemitones = 7),
            isChallenge = true,
        ),
    )
}
