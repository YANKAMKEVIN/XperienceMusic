package com.xperiencemusic.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignInLinkTest {
    @Test
    fun `le lien de l'e-mail donne l'adresse et le code`() {
        assertEquals(
            SignInLink("grace+louange@gmail.com", "042913"),
            SignInLink.parse("xperiencemusic://connexion?email=grace+louange%40gmail.com&code=042913"),
        )
        assertEquals(
            SignInLink("a@b.fr", "123456"),
            SignInLink.parse("XperienceMusic://connexion?code=123456&email=a@b.fr"),
        )
    }

    @Test
    fun `les autres liens sont ignorés`() {
        assertNull(SignInLink.parse("https://exemple.fr/connexion?email=a@b.fr&code=123456"))
        assertNull(SignInLink.parse("xperiencemusic://connexion?email=a@b.fr"))
        assertNull(SignInLink.parse("xperiencemusic://connexion?email=a@b.fr&code=12ab56"))
        assertNull(SignInLink.parse("xperiencemusic://autre?email=a@b.fr&code=123456"))
    }
}
