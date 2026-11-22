package com.xperiencemusic.shared.domain.help

import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpBlock
import com.xperiencemusic.shared.domain.model.HelpSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelpMarkdownTest {
    private val sample = """
        # Aide : titre de la page

        <!-- note pour les auteurs -->

        Une introduction
        sur deux lignes.

        <!-- hors-app -->
        Voir [l'autre page](autre.md).
        <!-- /hors-app -->

        ## Se connecter

        1. Tape ton adresse.
        2. Tape le code, ou touche
           le lien du mail.

        Un paragraphe après la liste.

        ### Le mode scène

        - **Importer** : colle `accords.app/JEM414`.
        - Deuxième puce
          continuée.

        ## Planning

        Fin.
    """.trimIndent()

    private val page = HelpMarkdown.parse(HelpAudience.MEMBERS, sample)

    @Test
    fun `le titre de la page et les commentaires disparaissent, l'introduction reste`() {
        assertEquals(listOf(HelpBlock.Paragraph(HelpMarkdown.inline("Une introduction sur deux lignes."))), page.intro)
    }

    @Test
    fun `les blocs hors-app restent sur GitHub`() {
        val all = (page.intro + page.sections.flatMap { it.blocks }).toString()
        assertTrue("autre page" !in all && "hors-app" !in all)
    }

    @Test
    fun `chaque titre de niveau 2 ouvre une section`() {
        assertEquals(listOf("Se connecter", "Planning"), page.sections.map { it.title })
        assertEquals(listOf(HelpBlock.Paragraph(HelpMarkdown.inline("Fin."))), page.sections[1].blocks)
    }

    @Test
    fun `listes numérotées, puces, sous-titres et lignes continuées`() {
        val blocks = page.sections[0].blocks
        assertEquals(
            listOf("Tape ton adresse.", "Tape le code, ou touche le lien du mail."),
            (blocks[0] as HelpBlock.Steps).items.map { it.plain },
        )
        assertEquals("Un paragraphe après la liste.", (blocks[1] as HelpBlock.Paragraph).text.plain)
        assertEquals("Le mode scène", (blocks[2] as HelpBlock.Subheading).text.plain)
        assertEquals(
            listOf("Importer : colle accords.app/JEM414.", "Deuxième puce continuée."),
            (blocks[3] as HelpBlock.Bullets).items.map { it.plain },
        )
        assertEquals(4, blocks.size)
    }

    @Test
    fun `le gras devient un segment à part, les liens gardent leur texte`() {
        assertEquals(
            listOf(HelpSpan("Admin++", bold = true), HelpSpan(" : voir la mise en place.")),
            HelpMarkdown.inline("**Admin++** : voir [la mise en place](../supabase-setup.md).").spans,
        )
    }
}
