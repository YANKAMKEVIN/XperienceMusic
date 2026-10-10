package com.xperiencemusic.shared.data.seed

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.Level

/** First draft of the singers' path, to be refined with the vocal coach. */
internal object SingerExercises {
    val all: List<Exercise> = listOf(
        Exercise(
            "b-breath", Level.BEGINNER, 1, "Souffle long",
            "Inspire par le ventre, puis expire sur un « sss » régulier le plus longtemps possible. " +
                "Enregistre-toi : objectif 20 secondes.",
            ExerciseCheck.Review,
        ),
        Exercise(
            "b-hold-comfort", Level.BEGINNER, 2, "Tenir ta note",
            "Chante ta note de confort sur « a » et tiens-la sans bouger.",
            ExerciseCheck.HoldNote(semitonesAboveComfort = 0, seconds = 5.0),
        ),
        Exercise(
            "b-hold-fifth", Level.BEGINNER, 3, "Monter à la quinte",
            "Même exercice, cinq notes au-dessus de ta note de confort.",
            ExerciseCheck.HoldNote(semitonesAboveComfort = 7, seconds = 5.0),
        ),
        Exercise(
            "b-scale", Level.BEGINNER, 4, "Gamme majeure",
            "Chante la gamme majeure montante puis descendante depuis ta note de confort.",
            ExerciseCheck.Review,
        ),
        Exercise(
            "c-hold-fifth", Level.CONFIRMED, 1, "Quinte tenue 10 s",
            "Tiens la quinte au-dessus de ta note de confort pendant 10 secondes, sans vibrato forcé.",
            ExerciseCheck.HoldNote(semitonesAboveComfort = 7, seconds = 10.0),
        ),
        Exercise(
            "c-hold-octave", Level.CONFIRMED, 2, "L'octave",
            "Atteins l'octave au-dessus de ta note de confort et tiens-la 6 secondes.",
            ExerciseCheck.HoldNote(semitonesAboveComfort = 12, seconds = 6.0),
        ),
        Exercise(
            "c-harmony", Level.CONFIRMED, 3, "Ta voix sur le chant de la semaine",
            "Chante ta partie (soprano, alto, ténor) sur un chant de la setlist et envoie l'enregistrement.",
            ExerciseCheck.Review,
        ),
        Exercise(
            "p-hold-octave", Level.PRO, 1, "Octave tenue 10 s",
            "Tiens l'octave au-dessus de ta note de confort pendant 10 secondes.",
            ExerciseCheck.HoldNote(semitonesAboveComfort = 12, seconds = 10.0),
        ),
        Exercise(
            "p-runs", Level.PRO, 2, "Vocalises et mélismes",
            "Enregistre une vocalise rapide sur la gamme pentatonique, propre et en rythme.",
            ExerciseCheck.Review,
        ),
        Exercise(
            "p-lead", Level.PRO, 3, "Conduire un chant",
            "Enregistre-toi en train de lancer et conduire un chant de la setlist, avec les relances.",
            ExerciseCheck.Review,
        ),
    )
}
