package uz.ishvaqtim.app.data

import android.content.Context

/** Profil Supabase'ga yozilgandan keyin olingan ID shu yerda (lokal) saqlanadi. */
object RemoteProfileStore {

    private const val PREFS_NAME = "ish_vaqtim_prefs"
    private const val KEY_REMOTE_PROFILE_ID = "remote_profile_id"

    fun get(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getLong(KEY_REMOTE_PROFILE_ID, -1L)
        return if (value == -1L) null else value
    }

    fun set(context: Context, id: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_REMOTE_PROFILE_ID, id).apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_REMOTE_PROFILE_ID).apply()
    }
}
