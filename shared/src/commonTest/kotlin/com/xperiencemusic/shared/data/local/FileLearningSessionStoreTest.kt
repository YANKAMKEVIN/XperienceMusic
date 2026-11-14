package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.model.LearningPreferences
import com.xperiencemusic.shared.domain.model.LearningResume
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileLearningSessionStoreTest {
    private val files = FakeFileStore()
    private val store = FileLearningSessionStore(SnapshotStore(files))

    @Test
    fun settingsAndResumePointsAreKeptPerMemberAndModule() {
        store.savePreferences("me", LearningPreferences(touchSound = false, weeklyTarget = 3))
        store.saveResume("me", "piano-chords", LearningResume(index = 2, notes = setOf(60), hints = 1))
        store.savePostponed("me", "singer-hold", setOf("singer-hold-sing"))

        assertEquals(LearningPreferences(touchSound = false, weeklyTarget = 3), store.preferences("me"))
        assertEquals(LearningPreferences(), store.preferences("other"))
        assertEquals(LearningResume(index = 2, notes = setOf(60), hints = 1), store.session("me", "piano-chords").resume)
        assertNull(store.session("me", "piano-scales").resume)
        assertEquals(setOf("singer-hold-sing"), store.session("me", "singer-hold").postponed)
        assertTrue(store.session("me", "piano-chords").postponed.isEmpty())
    }

    @Test
    fun filesWrittenByEarlierVersionsAreStillRead() {
        files.writeText("snapshots/learning-settings-me.json", """{"touchSound":false,"largeText":true,"startingPoint":"piano-chart"}""")
        files.writeText("snapshots/learning-resume-me-piano-chords.json", """{"index":1,"encounter":2,"notes":[60,64],"answer":"Majeur","hints":2}""")
        files.writeText("snapshots/learning-postponed-me-singer-hold.json", """["a","b"]""")

        assertEquals(LearningPreferences(touchSound = false, largeText = true, startingPoint = "piano-chart"), store.preferences("me"))
        assertEquals(LearningResume(1, 2, setOf(60, 64), "Majeur", 2), store.session("me", "piano-chords").resume)
        assertEquals(setOf("a", "b"), store.session("me", "singer-hold").postponed)
    }
}
