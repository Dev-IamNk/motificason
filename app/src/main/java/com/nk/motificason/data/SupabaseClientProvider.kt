package com.nk.motificason.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

object SupabaseClientProvider {
    const val SUPABASE_URL = "https://xzjzktrtprgffuyofuny.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_qsd__6HTyFAdTRht_fcYHQ_i7HSTTAd"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)
        }
    }
}
