package com.example.mindnest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindnest.data.entity.JournalEntity
import com.example.mindnest.model.JournalEntry
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _allJournals = MutableLiveData<List<JournalEntry>>(emptyList())
    val allJournals: LiveData<List<JournalEntry>> = _allJournals

    init {
        loadJournals()
    }

    fun reloadJournals() {
        loadJournals()
    }

    private fun loadJournals() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) {
            _allJournals.value = emptyList()
            return
        }

        viewModelScope.launch {
            app.journalRepository.getJournalEntriesByUser(userId)
                .map { entities ->
                    entities.map { entity ->
                        val day = SimpleDateFormat("dd", Locale.getDefault()).format(parseDate(entity.date))
                        val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(parseDate(entity.date))
                        val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            .format(parseDate(entity.date))
                            .uppercase()

                        JournalEntry(
                            id = entity.id,
                            day = day,
                            weekday = weekday,
                            text = entity.content,
                            monthYear = monthYear,
                            mood = entity.mood
                        )
                    }
                }
                .collect { list ->
                    _allJournals.value = list
                }
        }
    }

    fun addJournal(entry: JournalEntry) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            val entity = JournalEntity(
                id = 0,
                userId = userId,
                content = entry.text,
                mood = entry.mood,
                date = date
            )
            app.journalRepository.insertJournalEntry(entity)
        }
    }

    fun updateJournal(entry: JournalEntry) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || entry.id == 0L) return

        val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            val entity = JournalEntity(
                id = entry.id,
                userId = userId,
                content = entry.text,
                mood = entry.mood,
                date = date
            )
            app.journalRepository.updateJournalEntry(entity)
        }
    }

    private fun parseDate(dateStr: String): Date {
        // Stored format: dd/MM/yy
        return runCatching {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(dateStr)
        }.getOrNull() ?: Date()
    }
}

