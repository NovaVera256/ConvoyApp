package edu.temple.convoy

import android.content.Context

object SessionStore {
    private const val PREFS = "convoy_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_FIRST = "firstname"
    private const val KEY_LAST = "lastname"
    private const val KEY_SESSION = "session_key"

    fun isLoggedIn(ctx: Context): Boolean =
        getUsername(ctx) != null && getSessionKey(ctx) != null

    fun saveUser(ctx: Context, username: String, first: String?, last: String?) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_FIRST, first)
            .putString(KEY_LAST, last)
            .apply()
    }

    fun saveSessionKey(ctx: Context, sessionKey: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SESSION, sessionKey)
            .apply()
    }

    fun getUsername(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)

    fun getSessionKey(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SESSION, null)

    fun clearAll(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}