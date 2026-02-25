package com.example.mindnest

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PastSessionViewModel : ViewModel() {

    private val _pastSessions = MutableLiveData<MutableList<PastSession>>(mutableListOf())
    val pastSessions: LiveData<MutableList<PastSession>> = _pastSessions

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val PREFS_NAME = "mindful_sessions"
    private val KEY_SESSIONS = "sessions"

    private var listenerStarted = false


    var onSessionsUpdated: (() -> Unit)? = null

    fun addSession(session: PastSession, userId: Long, context: Context) {
        val uid = auth.currentUser?.uid ?: return

        val updatedList = loadLocalSessions(context, uid).toMutableList()

        updatedList.add(0, session)

        saveLocalSessions(context, uid, updatedList)
        _pastSessions.value = updatedList

        saveSessionToFirebase(session, uid)


        onSessionsUpdated?.invoke()
    }

    private fun saveSessionToFirebase(session: PastSession, uid: String) {
        val docId = session.startMillis.toString()
        firestore.collection("users")
            .document(uid)
            .collection("meditation_sessions")
            .document(docId)
            .set(
                mapOf(
                    "time" to session.time,
                    "date" to session.date,
                    "duration" to session.duration,
                    "startMillis" to session.startMillis
                )
            )
    }

    fun listenForRealtimeUpdates(context: Context, userId: Long) {
        val uid = auth.currentUser?.uid ?: return
        if (listenerStarted) return
        listenerStarted = true

        firestore.collection("users")
            .document(uid)
            .collection("meditation_sessions")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    val time = doc.getString("time") ?: return@mapNotNull null
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val duration = doc.getString("duration") ?: return@mapNotNull null
                    val startMillis = doc.getLong("startMillis") ?: 0L
                    PastSession(time, date, duration, startMillis)
                }.sortedByDescending { it.startMillis }.toMutableList()


                saveLocalSessions(context, uid, list)


                _pastSessions.postValue(list)

                onSessionsUpdated?.invoke()
            }
    }

    fun loadSessions(context: Context, userId: Long) {
        val uid = auth.currentUser?.uid ?: return
        val list = loadLocalSessions(context, uid).toMutableList()
        _pastSessions.value = list
    }

    private fun loadLocalSessions(context: Context, uid: String): List<PastSession> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_SESSIONS}_$uid"
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<PastSession>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun saveLocalSessions(context: Context, uid: String, list: List<PastSession>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_SESSIONS}_$uid"
        prefs.edit().putString(key, Gson().toJson(list)).apply()
    }

    fun saveOngoingSessionIfAny(session: PastSession?, context: Context) {
        session?.let { addSession(it, 0L, context) }
    }
}