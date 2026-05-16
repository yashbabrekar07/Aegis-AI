package com.aegisai.app

import android.app.Application
import com.aegisai.app.data.Prefs
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

class AegisApp : Application() {
    lateinit var prefs: Prefs
        private set

    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    companion object {
        fun get(context: android.content.Context): AegisApp =
            context.applicationContext as AegisApp
    }
}
