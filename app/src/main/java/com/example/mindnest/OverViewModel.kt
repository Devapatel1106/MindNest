package com.example.mindnest.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindnest.MindNestApplication
import com.example.mindnest.PastSession
import com.example.mindnest.data.entity.PeriodEntity
import com.example.mindnest.data.entity.SleepEntity
import com.example.mindnest.utils.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * OverviewViewModel loads summary data from the same repositories used by module fragments.
 * When user adds/deletes data in any module, Room (or prefs) updates and Flows emit,
 * so overview cards stay in sync.
 */
class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _taskSummary = MutableLiveData<String>("Loading…")
    val taskSummary: LiveData<String> = _taskSummary

    private val _waterSummary = MutableLiveData<String>("Loading…")
    val waterSummary: LiveData<String> = _waterSummary

    private val _journalSummary = MutableLiveData<String>("Loading…")
    val journalSummary: LiveData<String> = _journalSummary

    private val _periodSummary = MutableLiveData<String>("Loading…")
    val periodSummary: LiveData<String> = _periodSummary

    private val _sleepSummary = MutableLiveData<String>("Loading…")
    val sleepSummary: LiveData<String> = _sleepSummary

    private val _workoutSummary = MutableLiveData<String>("Loading…")
    val workoutSummary: LiveData<String> = _workoutSummary

    private val _calorieSummary = MutableLiveData<String>("Loading…")
    val calorieSummary: LiveData<String> = _calorieSummary

    private val _meditationSummary = MutableLiveData<String>("Loading…")
    val meditationSummary: LiveData<String> = _meditationSummary

    init {
        startObservingModules()
        refreshMeditation()
        refreshCalories()
    }

    private fun startObservingModules() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) {
            setDefaultsNoUser()
            return
        }

        // Tasks: live from Room
        viewModelScope.launch {
            app.taskRepository.getTasksByUser(userId).collect { list ->
                val pending = list.count { !it.completed }
                _taskSummary.value = when {
                    list.isEmpty() -> "No tasks"
                    pending == 0 -> "All done"
                    else -> "$pending pending"
                }
            }
        }

        // Water: entries + target from Room
        viewModelScope.launch {
            val today = todayDateString()
            combine(
                app.waterRepository.getWaterEntriesByUser(userId),
                app.userSettingsRepository.getUserSettings(userId)
            ) { entries, settings ->
                val target = settings?.waterTargetMl ?: 0
                val todayMl = entries.filter { it.date == today }.sumOf { it.amountMl }
                when {
                    target <= 0 -> "Set target"
                    else -> "$todayMl / $target ml"
                }
            }.collect { _waterSummary.value = it }
        }

        // Journal: live from Room
        viewModelScope.launch {
            app.journalRepository.getJournalEntriesByUser(userId).collect { list ->
                _journalSummary.value = when {
                    list.isEmpty() -> "No entries"
                    else -> "Last • ${list.last().mood}"
                }
            }
        }

        // Period: live from Room → "Ongoing period" or "Next period on dd/MM/yy"
        viewModelScope.launch {
            app.periodRepository.getPeriodTracking(userId).collect { period ->
                _periodSummary.value = formatPeriodSummary(period)
            }
        }

        // Sleep: live from Room — current day only
        viewModelScope.launch {
            app.sleepRepository.getSleepLogsByUser(userId).collect { list ->
                val today = todayDateString()
                val todayLogs = list.filter { it.date == today }
                if (todayLogs.isEmpty()) {
                    _sleepSummary.value = "No logs today"
                } else {
                    // Sum or show latest: one log per day typically, show duration of latest today
                    val latest = todayLogs.last()
                    _sleepSummary.value = formatSleepDuration(latest)
                }
            }
        }

        // Workout: live from Room — today's minutes + total minutes
        viewModelScope.launch {
            app.workoutRepository.getWorkoutsByUser(userId).collect { list ->
                if (list.isEmpty()) {
                    _workoutSummary.value = "No workout"
                } else {
                    val (startOfToday, endOfToday) = todayMillisRange()
                    val todayMinutes = list.filter { it.date in startOfToday..endOfToday }.sumOf { it.durationMinutes }
                    val totalMinutes = list.sumOf { it.durationMinutes }
                    _workoutSummary.value = when {
                        totalMinutes == 0 -> "No workout"
                        todayMinutes == totalMinutes -> "$totalMinutes min"
                        todayMinutes == 0 -> "$totalMinutes min total"
                        else -> "$todayMinutes min today • $totalMinutes total"
                    }
                }
            }
        }
    }

    private fun setDefaultsNoUser() {
        _taskSummary.value = "No tasks"
        _waterSummary.value = "Set target"
        _journalSummary.value = "No entries"
        _periodSummary.value = "Not tracked"
        _sleepSummary.value = "No logs"
        _workoutSummary.value = "No workout"
        _calorieSummary.value = "0 kcal"
        _meditationSummary.value = "0 sessions"
    }

    private fun formatPeriodSummary(period: PeriodEntity?): String {
        if (period == null || period.startDate == null) return "Not tracked"
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
        val today = LocalDate.now()
        val start = try {
            LocalDate.parse(period.startDate, formatter)
        } catch (e: Exception) { return "Not tracked" }
        val end = period.endDate?.let { try { LocalDate.parse(it, formatter) } catch (e: Exception) { null } }
        val cycleLength = period.cycleLength

        return when {
            end == null -> {
                if (today.isBefore(start) || today.isEqual(start)) "Next period on ${start.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
                else "Ongoing period"
            }
            today.isBefore(start) -> "Next period on ${start.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
            today.isAfter(end) -> {
                val next = start.plusDays(cycleLength.toLong())
                "Next period on ${next.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
            }
            else -> "Ongoing period"
        }
    }

    private fun formatSleepDuration(entity: SleepEntity): String {
        var startM = entity.startHour * 60 + entity.startMinute
        var endM = entity.endHour * 60 + entity.endMinute
        if (endM <= startM) endM += 24 * 60
        val diff = endM - startM
        val h = diff / 60
        val m = diff % 60
        return "${h}h ${m}m"
    }

    private fun todayDateString(): String = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

    /** Returns start of today and end of today in millis (for workout date filter). */
    private fun todayMillisRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000 - 1
        return Pair(start, end)
    }

    fun refreshAll() {
        refreshMeditation()
        refreshCalories()
    }

    fun refreshMeditationCount() = refreshMeditation()
    fun refreshMeditation() {
        viewModelScope.launch {
            _meditationSummary.value = loadMeditationSummary()
        }
    }

    private fun loadMeditationSummary(): String {
        val appContext = getApplication<Application>()
        val prefs = appContext.getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)
        val today = todayDateString()

        // Try userId from user_prefs (Int) - same as MindfulnessSessionFragment
        val userIdFromUserPrefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("user_id", -1)
        val sessionsFromUserPrefs = parseSessionsAndCountToday(prefs, "sessions_$userIdFromUserPrefs", today)
        if (sessionsFromUserPrefs >= 0) {
            return when {
                sessionsFromUserPrefs == 0 -> "0 sessions"
                sessionsFromUserPrefs == 1 -> "1 session"
                else -> "$sessionsFromUserPrefs sessions"
            }
        }

        // Fallback: userId from MindNestPrefs (Long as Int)
        val userIdLong = preferenceManager.getUserId()
        if (userIdLong > 0) {
            val count = parseSessionsAndCountToday(prefs, "sessions_${userIdLong.toInt()}", today)
            if (count >= 0) {
                return when {
                    count == 0 -> "0 sessions"
                    count == 1 -> "1 session"
                    else -> "$count sessions"
                }
            }
        }

        return "0 sessions"
    }

    private fun parseSessionsAndCountToday(prefs: android.content.SharedPreferences, key: String, today: String): Int {
        val json = prefs.getString(key, null) ?: return -1
        if (json.isEmpty()) return 0
        return try {
            val type = object : TypeToken<MutableList<PastSession>>() {}.type
            val list: MutableList<PastSession> = Gson().fromJson(json, type)
            list.count { it.date == today }
        } catch (e: Exception) { -1 }
    }

    private fun refreshCalories() {
        viewModelScope.launch {
            val userId = preferenceManager.getUserId().toString()
            val today = java.time.LocalDate.now().toString()
            val list = app.calorieRepository.getTodayFood(userId, today)
            val total = list.sumOf { it.calories * it.quantity }
            _calorieSummary.value = "$total kcal"
        }
    }

    fun notifyTasksChanged() { viewModelScope.launch { app.taskRepository.getTasksByUser(preferenceManager.getUserId()).first { true } } }
    fun notifyWaterChanged() { /* Flow already observed */ }
    fun notifyJournalChanged() { /* Flow already observed */ }
    fun notifyPeriodChanged() { /* Flow already observed */ }
    fun notifySleepChanged() { /* Flow already observed */ }
    fun notifyWorkoutChanged() { /* Flow already observed */ }
    fun notifyMeditationChanged() = refreshMeditation()
    fun notifyCaloriesChanged() = refreshCalories()
}