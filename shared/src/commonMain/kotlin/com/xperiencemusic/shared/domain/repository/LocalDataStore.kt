package com.xperiencemusic.shared.domain.repository

/** Everything the app keeps on the phone about the band. Wiped at sign-out: phones get shared. */
fun interface LocalDataStore {
    suspend fun clearAll()
}
