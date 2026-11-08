package com.xperiencemusic.shared.domain.learning

/**
 * "0 note", "1 note", "2 notes": in French, zero and one take the singular. [other] defaults to
 * [one] with an s, which covers whole phrases such as "note correcte" → "notes correctes" only
 * when every word takes an s; pass it explicitly otherwise.
 */
fun frenchCount(count: Int, one: String, other: String = one.split(' ').joinToString(" ") { it + "s" }): String =
    "$count ${if (count <= 1) one else other}"
