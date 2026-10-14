package com.xperiencemusic.shared.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportSongFromLinkUseCaseTest {
    @Test
    fun `accepte les liens d'un chant d'accords app sous toutes leurs formes`() {
        assertEquals("https://accords.app/JEM414", accordsPageUrl("https://accords.app/JEM414?gamme=Do"))
        assertEquals("https://accords.app/JEM414", accordsPageUrl("  accords.app/JEM414/ "))
        assertEquals("https://accords.app/JEM414", accordsPageUrl("http://www.accords.app/JEM414#haut"))
        assertEquals("https://accords.app/414-grandes-et-merveilleuses", accordsPageUrl("accords.app/414-grandes-et-merveilleuses"))
    }

    @Test
    fun `refuse les autres sites et les pages de catalogue`() {
        assertNull(accordsPageUrl("https://exemple.com/JEM414"))
        assertNull(accordsPageUrl("https://accords.app.exemple.com/JEM414"))
        assertNull(accordsPageUrl("https://accords.app/tousleschants"))
        assertNull(accordsPageUrl("https://accords.app/liste/abc"))
        assertNull(accordsPageUrl("https://accords.app/"))
    }
}
