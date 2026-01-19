package com.example.mindnest.utils

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "mindnest_session"
    private const val KEY_USER_ID = "user_id"

    fun saveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context) != -1L
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
