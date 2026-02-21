package com.example.mindnest

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PastSessionViewModel : ViewModel() {

    private val _pastSessions = MutableLiveData<MutableList<PastSession>>(mutableListOf())
    val pastSessions: LiveData<MutableList<PastSession>> = _pastSessions

    private val firestore = FirebaseFirestore.getInstance()

    private val PREFS_NAME = "mindful_sessions"
    private val KEY_SESSIONS = "sessions"

    fun addSession(session: PastSession, userId: Int, context: Context) {

        if (userId <= 0) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val key = "${KEY_SESSIONS}_$userId"

        val json = prefs.getString(key, null)
        val type = object : TypeToken<MutableList<PastSession>>() {}.type

        val list: MutableList<PastSession> =
            if (!json.isNullOrEmpty()) gson.fromJson(json, type)
            else mutableListOf()

        list.add(0, session)

        prefs.edit().putString(key, gson.toJson(list)).apply()

        _pastSessions.value = list

        saveSessionToFirebase(session, userId)
    }


    private fun saveSessionToFirebase(session: PastSession, userId: Int) {

        firestore.collection("meditation_sessions")
            .document(userId.toString())
            .collection("sessions")
            .add(
                mapOf(
                    "time" to session.time,
                    "date" to session.date,
                    "duration" to session.duration,
                    "startMillis" to session.startMillis
                )
            )
    }


    fun loadSessions(context: Context, userId: Int) {

        if (userId <= 0) {
            _pastSessions.value = mutableListOf()
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_SESSIONS}_$userId"
        val json = prefs.getString(key, null)

        if (!json.isNullOrEmpty()) {

            val gson = Gson()
            val type = object : TypeToken<MutableList<PastSession>>() {}.type

            _pastSessions.value = gson.fromJson(json, type)

        } else {
            _pastSessions.value = mutableListOf()
        }
    }
}