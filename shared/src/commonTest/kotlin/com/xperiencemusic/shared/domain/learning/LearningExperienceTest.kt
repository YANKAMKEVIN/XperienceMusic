package com.xperiencemusic.shared.domain.learning

import kotlinx.datetime.LocalDate
import kotlin.test.*

class LearningExperienceTest {
    private fun attempt(day:Int,passed:Boolean=true,hints:Int=0)=LearningAttempt("$day-$passed-$hints","me",PianoBasics.REVIEW_ID,1,passed,LocalDate(2026,10,day),hints=hints)
    @Test fun revisionsExpandOnlyAfterUnassistedSuccessesOnDifferentDays() {
        assertEquals(LocalDate(2026,10,21),LearningProgress(listOf(attempt(18))).reviewDate(PianoBasics.REVIEW_ID))
        assertEquals(LocalDate(2026,10,21),LearningProgress(listOf(attempt(18),attempt(18))).reviewDate(PianoBasics.REVIEW_ID))
        assertEquals(LocalDate(2026,10,26),LearningProgress(listOf(attempt(18),attempt(19))).reviewDate(PianoBasics.REVIEW_ID))
        assertEquals(LocalDate(2026,10,21),LearningProgress(listOf(attempt(18),attempt(19),attempt(20,false))).reviewDate(PianoBasics.REVIEW_ID))
        assertEquals(LocalDate(2026,10,21),LearningProgress(listOf(attempt(20,hints=1))).reviewDate(PianoBasics.REVIEW_ID))
    }
    @Test fun variantsKeepCorrectIntervalsAndStayInsideKeyboard() {
        PianoBasics.activities.filter{it.kind==LearningKind.KEYBOARD}.forEach { base ->
            for(n in 1..15) {
                val varied=variedActivity(base,n)
                assertEquals(base.expectedNotes.sorted().zipWithNext{a,b->b-a},varied.expectedNotes.sorted().zipWithNext{a,b->b-a})
                assertTrue(varied.expectedNotes.all{it in 60..84})
            }
        }
        val ear=PianoBasics.activities.first{it.kind==LearningKind.EAR}
        assertEquals(setOf("Majeur","Mineur"),(1..8).map{variedActivity(ear,it).answer}.toSet())
    }
    @Test fun feedbackIdentifiesTheWrongThirdAndRetainsOtherNotes() {
        val minor=PianoBasics.activities.first{it.id=="piano-chords-build-minor"}
        val text=correction(minor,setOf(60,64,67),null)
        assertTrue(text.contains("Mi4"));assertTrue(text.contains("Mi♭4"))
    }
    @Test fun rhythmMeasuresIntervalsAndRejectsIrregularOrIncompleteAttempts() {
        assertNull(rhythmError(listOf(0.0,1.0),60))
        assertEquals(0.0,rhythmError((0..7).map{it.toDouble()},60)!!,0.001)
        assertTrue(rhythmError(listOf(0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7),60)!!>0.18)
    }
    @Test fun modulesHaveDistinctActivitiesAndObjectiveLinks() {
        assertEquals(5,LearningCatalog.modules.size)
        val activities=LearningCatalog.modules.flatMap{it.activities}
        assertEquals(activities.size,activities.map{it.id}.distinct().size)
        assertEquals(5,LearningCatalog.modules.map{it.objectiveId}.distinct().size)
        val plan=LearningProgress().dailyPlan(LearningCatalog.modules.first(),LocalDate(2026,10,18))
        assertEquals(listOf(0,1,2),plan)
    }
}
