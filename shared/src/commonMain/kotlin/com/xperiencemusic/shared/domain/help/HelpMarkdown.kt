package com.xperiencemusic.shared.domain.help

import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpBlock
import com.xperiencemusic.shared.domain.model.HelpPage
import com.xperiencemusic.shared.domain.model.HelpSection
import com.xperiencemusic.shared.domain.model.HelpSpan
import com.xperiencemusic.shared.domain.model.HelpText

/**
 * Reads the small part of Markdown the help pages of `docs/aide` use: `#`, `##` and `###` headings,
 * paragraphs, `-` lists, numbered lists (continued by indented lines), `**bold**`, `` `code` `` and
 * links, of which only the text stays. Blocks between `<!-- hors-app -->` and `<!-- /hors-app -->`
 * (links to the other page or to the setup notes) stay on GitHub; other HTML comments are dropped.
 */
object HelpMarkdown {
    private val webOnly = Regex("""<!--\s*hors-app\s*-->[\s\S]*?<!--\s*/hors-app\s*-->""")
    private val comment = Regex("""<!--[\s\S]*?-->""")
    private val bullet = Regex("""^[-*]\s+(.*)$""")
    private val step = Regex("""^\d+\.\s+(.*)$""")
    private val link = Regex("""\[([^\]]*)]\([^)]*\)""")

    fun parse(audience: HelpAudience, markdown: String): HelpPage {
        val builder = PageBuilder()
        markdown.replace("\r", "").replace(webOnly, "").replace(comment, "").lines().forEach(builder::read)
        return builder.build(audience)
    }

    /** `**bold**` becomes a bold span; links keep their text and code its content. */
    fun inline(text: String): HelpText {
        val flat = link.replace(text) { it.groupValues[1] }.replace("`", "")
        val spans = flat.split("**").mapIndexedNotNull { index, part ->
            part.takeIf { it.isNotEmpty() }?.let { HelpSpan(it, bold = index % 2 == 1) }
        }
        return HelpText(spans)
    }

    private enum class ListKind { BULLETS, STEPS }

    private class PageBuilder {
        private val intro = mutableListOf<HelpBlock>()
        private val sections = mutableListOf<HelpSection>()
        private var sectionTitle: String? = null
        private var blocks = intro
        private val paragraph = mutableListOf<String>()
        private var listKind: ListKind? = null
        private val items = mutableListOf<String>()

        fun read(raw: String) {
            val line = raw.trim()
            val listItem = bullet.find(line)?.let { ListKind.BULLETS to it } ?: step.find(line)?.let { ListKind.STEPS to it }
            when {
                line.isEmpty() -> flushParagraph()
                line.startsWith("#") -> heading(line)
                listItem != null -> {
                    flushParagraph()
                    if (listKind != listItem.first) flushList()
                    listKind = listItem.first
                    items += listItem.second.groupValues[1]
                }
                listKind != null && raw.startsWith(" ") && items.isNotEmpty() && paragraph.isEmpty() ->
                    items[items.lastIndex] = items.last() + " " + line
                else -> {
                    flushList()
                    paragraph += line
                }
            }
        }

        /** `##` opens a section, `###` is a subheading in it; the page's `#` title is not kept. */
        private fun heading(line: String) {
            flushAll()
            val level = line.takeWhile { it == '#' }.length
            val text = line.drop(level).trim()
            when (level) {
                1 -> Unit
                2 -> {
                    closeSection()
                    sectionTitle = inline(text).plain
                    blocks = mutableListOf()
                }
                else -> blocks += HelpBlock.Subheading(inline(text))
            }
        }

        fun build(audience: HelpAudience): HelpPage {
            flushAll()
            closeSection()
            return HelpPage(audience, intro.toList(), sections.toList())
        }

        private fun flushParagraph() {
            if (paragraph.isEmpty()) return
            blocks += HelpBlock.Paragraph(inline(paragraph.joinToString(" ")))
            paragraph.clear()
        }

        private fun flushList() {
            val kind = listKind ?: return
            val texts = items.map(::inline)
            blocks += if (kind == ListKind.BULLETS) HelpBlock.Bullets(texts) else HelpBlock.Steps(texts)
            items.clear()
            listKind = null
        }

        private fun flushAll() {
            flushParagraph()
            flushList()
        }

        private fun closeSection() {
            val title = sectionTitle ?: return
            sections += HelpSection(title, blocks.toList())
        }
    }
}
