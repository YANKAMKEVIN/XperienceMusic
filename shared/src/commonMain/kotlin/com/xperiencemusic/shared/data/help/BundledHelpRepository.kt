package com.xperiencemusic.shared.data.help

import com.xperiencemusic.shared.domain.help.HelpMarkdown
import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpPage
import com.xperiencemusic.shared.domain.repository.HelpRepository

/** Reads `docs/aide`, compiled into the app by `:shared:generateHelpPages`. Each page is parsed once. */
class BundledHelpRepository : HelpRepository {
    private val pages = HelpAudience.entries.associateWith { audience ->
        lazy { HelpMarkdown.parse(audience, source(audience)) }
    }

    override fun page(audience: HelpAudience): HelpPage = pages.getValue(audience).value

    private fun source(audience: HelpAudience): String = when (audience) {
        HelpAudience.MEMBERS -> HelpSources.MEMBERS
        HelpAudience.LEADERS -> HelpSources.LEADERS
    }
}
