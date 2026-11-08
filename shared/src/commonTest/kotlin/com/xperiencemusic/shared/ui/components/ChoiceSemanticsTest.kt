package com.xperiencemusic.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ChoiceSemanticsTest {
    @Test
    fun `le lecteur d'écran dit si le choix est pris`() {
        assertEquals("sélectionné", choiceStateLabel(true))
        assertEquals("non sélectionné", choiceStateLabel(false))
    }
}
