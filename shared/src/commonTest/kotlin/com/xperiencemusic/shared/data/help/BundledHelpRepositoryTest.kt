package com.xperiencemusic.shared.data.help

import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpBlock
import com.xperiencemusic.shared.domain.model.HelpText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The real pages of docs/aide, as compiled into the app. */
class BundledHelpRepositoryTest {
    private val repository = BundledHelpRepository()

    private fun texts(audience: HelpAudience): List<HelpText> {
        val page = repository.page(audience)
        return (page.intro + page.sections.flatMap { it.blocks }).flatMap { block ->
            when (block) {
                is HelpBlock.Paragraph -> listOf(block.text)
                is HelpBlock.Subheading -> listOf(block.text)
                is HelpBlock.Bullets -> block.items
                is HelpBlock.Steps -> block.items
            }
        }
    }

    @Test
    fun `la page des membres suit docs-aide-membres`() {
        val page = repository.page(HelpAudience.MEMBERS)
        assertEquals(
            listOf("Se connecter", "Semaine", "Chants", "Le lecteur", "Planning", "Ma progression", "Sans réseau"),
            page.sections.map { it.title },
        )
        assertTrue(page.sections.all { it.blocks.isNotEmpty() })
    }

    @Test
    fun `la page des responsables suit docs-aide-responsable`() {
        val page = repository.page(HelpAudience.LEADERS)
        assertEquals("Publier la setlist de la semaine", page.sections.first().title)
        assertEquals("Bon à savoir", page.sections.last().title)
        assertTrue(page.sections.all { it.blocks.isNotEmpty() })
    }

    @Test
    fun `aucune marque Markdown ni lien de GitHub n'arrive à l'écran`() {
        HelpAudience.entries.forEach { audience ->
            texts(audience).map { it.plain }.forEach { text ->
                listOf("**", "](", "<!--", "`", ".md", "supabase-setup").forEach { mark ->
                    assertTrue(mark !in text, "« $mark » dans l'aide $audience : $text")
                }
            }
        }
    }

    @Test
    fun `le gras de la page des responsables est gardé`() {
        val intro = repository.page(HelpAudience.LEADERS).intro.first() as HelpBlock.Paragraph
        assertEquals(listOf("Admin contenu", "Admin++"), intro.text.spans.filter { it.bold }.map { it.text })
    }
}
