package com.xperiencemusic.shared.domain.model

/** Who a help page is written for: one page per audience, from `docs/aide`. */
enum class HelpAudience {
    /** Singers and musicians: `docs/aide/membres.md`. */
    MEMBERS,

    /** Content admins and Admin++: `docs/aide/responsable.md`. */
    LEADERS,
}

/**
 * The help pages a role reads, the one to open first at the head. Members only read theirs; the
 * leaders read their own page first, then the members' one, since they use the app as members too.
 */
fun Role.helpAudiences(): List<HelpAudience> = when (this) {
    Role.MEMBER -> listOf(HelpAudience.MEMBERS)
    Role.CONTENT_ADMIN, Role.SUPER_ADMIN -> listOf(HelpAudience.LEADERS, HelpAudience.MEMBERS)
}

/** One help page: the text before its first heading, then its sections. Its `#` title is not kept. */
data class HelpPage(
    val audience: HelpAudience,
    val intro: List<HelpBlock>,
    val sections: List<HelpSection>,
)

data class HelpSection(val title: String, val blocks: List<HelpBlock>)

sealed interface HelpBlock {
    data class Paragraph(val text: HelpText) : HelpBlock

    /** A `###` heading inside a section. */
    data class Subheading(val text: HelpText) : HelpBlock

    data class Bullets(val items: List<HelpText>) : HelpBlock

    /** A numbered list: steps to follow in order. */
    data class Steps(val items: List<HelpText>) : HelpBlock
}

/** A line of help text, cut where the bold starts and stops. */
data class HelpText(val spans: List<HelpSpan>) {
    val plain: String get() = spans.joinToString("") { it.text }
}

data class HelpSpan(val text: String, val bold: Boolean = false)
