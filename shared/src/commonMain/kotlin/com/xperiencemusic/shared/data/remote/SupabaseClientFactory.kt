package com.xperiencemusic.shared.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

internal fun createXperienceSupabaseClient(): SupabaseClient =
    createSupabaseClient(supabaseUrl = SupabaseConfig.URL, supabaseKey = SupabaseConfig.PUBLISHABLE_KEY) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

internal const val AUDIO_BUCKET = "audio"
