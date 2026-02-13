package com.example.mindnest

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PastSessionViewModel : ViewModel() {

    private val _pastSessions = MutableLiveData<MutableList<PastSession>>(mutableListOf())
    val pastSessions: LiveData<MutableList<PastSession>> = _pastSessions

    private val PREFS_NAME = "mindful_sessions"
    private val KEY_SESSIONS = "sessions"

    fun addSession(session: PastSession, userId: Int, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        val json = prefs.getString("${KEY_SESSIONS}_$userId", null)
        val type = object : TypeToken<MutableList<PastSession>>() {}.type

        val list: MutableList<PastSession> =
            if (!json.isNullOrEmpty()) gson.fromJson(json, type)
            else mutableListOf()


        list.add(0, session)


        prefs.edit().putString("${KEY_SESSIONS}_$userId", gson.toJson(list)).apply()


        _pastSessions.value = list
    }

    fun loadSessions(context: Context, userId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("${KEY_SESSIONS}_$userId", null)

        if (!json.isNullOrEmpty()) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<PastSession>>() {}.type
            _pastSessions.value = gson.fromJson(json, type)
        } else {
            _pastSessions.value = mutableListOf()
        }
    }
}
