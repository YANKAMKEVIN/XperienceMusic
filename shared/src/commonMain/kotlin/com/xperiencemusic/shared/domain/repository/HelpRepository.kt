package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpPage

/** The help pages shipped with the app: the same text for every account, readable offline. */
interface HelpRepository {
    fun page(audience: HelpAudience): HelpPage
}
