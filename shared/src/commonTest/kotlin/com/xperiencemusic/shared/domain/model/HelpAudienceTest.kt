package com.xperiencemusic.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class HelpAudienceTest {
    @Test
    fun `un membre ne lit que l'aide des membres`() {
        assertEquals(listOf(HelpAudience.MEMBERS), Role.MEMBER.helpAudiences())
    }

    @Test
    fun `les responsables lisent d'abord leur aide puis celle des membres`() {
        val both = listOf(HelpAudience.LEADERS, HelpAudience.MEMBERS)
        assertEquals(both, Role.CONTENT_ADMIN.helpAudiences())
        assertEquals(both, Role.SUPER_ADMIN.helpAudiences())
    }
}
