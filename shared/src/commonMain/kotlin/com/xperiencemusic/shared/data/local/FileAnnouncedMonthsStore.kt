package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.AnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.FileStore
import kotlinx.datetime.LocalDate

class FileAnnouncedMonthsStore(private val files: FileStore) : AnnouncedMonthsStore {
    override fun load(): Set<LocalDate> =
        files.readText(PATH)?.lines()?.mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }?.toSet().orEmpty()

    override fun save(months: Set<LocalDate>) = files.writeText(PATH, months.sorted().joinToString("\n"))

    companion object {
        const val DIRECTORY = "reminders"
        private const val PATH = "$DIRECTORY/announced.txt"
    }
}
