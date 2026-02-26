package com.example.mindnest.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.mindnest.MindNestApplication
import com.example.mindnest.PastSession
import com.example.mindnest.data.entity.MindScoreEntity
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
import java.util.*

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _taskSummary = MutableLiveData("Loading…")
    val taskSummary: LiveData<String> = _taskSummary

    private val _waterSummary = MutableLiveData("Loading…")
    val waterSummary: LiveData<String> = _waterSummary

    private val _journalSummary = MutableLiveData("Loading…")
    val journalSummary: LiveData<String> = _journalSummary

    private val _periodSummary = MutableLiveData("Loading…")
    val periodSummary: LiveData<String> = _periodSummary

    private val _sleepSummary = MutableLiveData("Loading…")
    val sleepSummary: LiveData<String> = _sleepSummary

    private val _workoutSummary = MutableLiveData("Loading…")
    val workoutSummary: LiveData<String> = _workoutSummary

    private val _calorieSummary = MutableLiveData("Loading…")
    val calorieSummary: LiveData<String> = _calorieSummary

    private val _meditationSummary = MutableLiveData("Loading…")
    val meditationSummary: LiveData<String> = _meditationSummary

    private val _mindScore = MutableLiveData(0)
    val mindScore: LiveData<Int> = _mindScore

    private val _mindScoreStatus = MutableLiveData("")
    val mindScoreStatus: LiveData<String> = _mindScoreStatus


    init {
        viewModelScope.launch {
            initUserId()
            startRealtimeSync()
            startObservingModules()
            refreshMeditation()
            refreshCalories()
            refreshMindScore()
        }
    }

    private suspend fun initUserId() {
        if (preferenceManager.getUserId() > 0) return
        val email = preferenceManager.getUserEmail()
        if (!email.isNullOrEmpty()) {
            val user = app.userRepository.getUserByEmail(email)
            if (user != null) {
                preferenceManager.saveUserId(user.id)
                preferenceManager.saveUserName(user.name)
                preferenceManager.saveUserEmail(user.email)
                preferenceManager.saveUserGender(user.gender)
            }
        }
    }

    private fun startObservingModules() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            app.taskRepository.getTasksByUser(userId).collect { list ->
                val today = todayDateString()
                val todayTasks = list.filter { it.date == today }
                val completed = todayTasks.count { it.completed }

                _taskSummary.value = when {
                    todayTasks.isEmpty() -> "No tasks"
                    else -> "$completed of ${todayTasks.size} completed"
                }
            }
        }

        viewModelScope.launch {
            combine(
                app.waterRepository.getWaterEntriesByUser(userId),
                app.userSettingsRepository.getUserSettings(userId)
            ) { entries, settings ->
                val today = todayDateString()
                val targetMl = settings?.waterTargetMl ?: 2000
                val todayMl = entries.filter { it.date == today }.sumOf { it.amountMl }
                if (targetMl <= 0) return@combine "Set target"
                "$todayMl / $targetMl ml"
            }.collect { _waterSummary.value = it }
        }

        viewModelScope.launch {
            app.journalRepository
                .getJournalEntryByDate(userId, todayDateString())
                .collect { entry ->
                    _journalSummary.value =
                        if (entry == null) "Log today's mood"
                        else "${entry.mood?.replaceFirstChar { it.uppercase() } ?: "Mood"} mood logged"
                }
        }

        viewModelScope.launch { app.periodRepository.getPeriodTracking(userId).collect { _periodSummary.value = formatPeriodSummary(it) } }
        viewModelScope.launch {
            app.sleepRepository.getSleepLogsByUser(userId).collect { list ->

                val today = todayDateString()
                val todayLogs = list.filter { it.date == today }

                if (todayLogs.isEmpty()) {
                    _sleepSummary.value = "No logs today"
                    return@collect
                }

                var totalMinutes = 0

                todayLogs.forEach { entity ->
                    var start = entity.startHour * 60 + entity.startMinute
                    var end = entity.endHour * 60 + entity.endMinute

                    if (end <= start) end += 24 * 60

                    totalMinutes += (end - start)
                }

                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60

                _sleepSummary.value = "${hours}h ${minutes}m"
            }
        }
        viewModelScope.launch { app.workoutRepository.getWorkoutsByUser(userId).collect { list ->
            val (start, end) = todayMillisRange()
            val todayWorkouts = list.filter { it.date in start..end }
            _workoutSummary.value = if (todayWorkouts.isEmpty()) "No workout" else "${todayWorkouts.size} workouts"
        }}
    }





    fun refreshAll() {
        refreshMeditation()
        refreshCalories()
        refreshMindScore()
    }

    fun notifyTasksChanged() = refreshMindScore()
    fun notifyWaterChanged() = refreshMindScore()
    fun notifyJournalChanged() = refreshMindScore()
    fun notifyPeriodChanged() = refreshMindScore()
    fun notifySleepChanged() = refreshMindScore()
    fun notifyWorkoutChanged() = refreshMindScore()

    fun notifyMeditationChanged() {
        refreshMeditation()
        refreshMindScore()
    }

    fun notifyCaloriesChanged() {
        refreshCalories()
        refreshMindScore()
    }



    private fun refreshMeditation() {
        viewModelScope.launch {
            _meditationSummary.value = loadMeditationSummary()
        }
    }

    private fun loadMeditationSummary(): String {
        val prefs = getApplication<Application>()
            .getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)

        val firebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return "0 sessions"

        val json = prefs.getString("sessions_$firebaseUid", null) ?: return "0 sessions"

        val type = object : com.google.gson.reflect.TypeToken<MutableList<PastSession>>() {}.type
        val list: MutableList<PastSession> = com.google.gson.Gson().fromJson(json, type) ?: mutableListOf()

        val today = todayDateString()
        val count = list.count { it.date == today }

        return "$count sessions"
    }


    private fun refreshCalories() {

        viewModelScope.launch {

            val userId = preferenceManager.getUserId().toString()
            val today = LocalDate.now().toString()

            val list = app.calorieRepository
                .getTodayFood(userId, today)
                .first()

            val total = list.sumOf { it.calories * it.quantity }

            val userInfo = app.calorieRepository.getUser(userId)

            val target = userInfo?.targetCalories ?: 2000

            _calorieSummary.value = "$total / $target kcal"
        }
    }



    private fun refreshMindScore() {

        viewModelScope.launch {

            val userId = preferenceManager.getUserId()

            if (userId <= 0) {
                _mindScore.postValue(0)
                _mindScoreStatus.postValue("")
                return@launch
            }

            val today = todayDateString()

            if (!hasAnyMindData(userId, today)) {
                _mindScore.postValue(0)
                _mindScoreStatus.postValue("")
                return@launch
            }

            val emotional = computeEmotionalScore(userId, today)
            val sleep = computeSleepScore(userId, today)
            val meditation = computeMeditationScore(userId, today)
            val water = computeWaterScore(userId, today)
            val physical = computePhysicalScore(userId)
            val tasks = computeTaskScore(userId, today)

            val score =
                (emotional * 0.12 +
                        sleep * 0.20 +
                        meditation * 0.15 +
                        water * 0.15 +
                        physical * 0.20 +
                        tasks * 0.18).toInt()

            val finalScore = score.coerceIn(0, 100)

            _mindScore.postValue(finalScore)
            _mindScoreStatus.postValue(interpretScore(finalScore))

            val todayDbFormat = convertToDbDateFormat(today)

            val existing =
                app.mindScoreRepository.getScoreByDate(userId, todayDbFormat)

            if (existing != finalScore) {
                app.mindScoreRepository.insertScore(
                    MindScoreEntity(
                        userId = userId,
                        date = todayDbFormat,
                        score = finalScore
                    )
                )
            }

        }
    }

    fun getLast7DaysMindScores(): LiveData<List<Pair<String, Int>>> {
        val result = MutableLiveData<List<Pair<String, Int>>>()

        viewModelScope.launch {
            val userId = preferenceManager.getUserId()
            if (userId <= 0) {
                result.postValue(emptyList())
                return@launch
            }

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)

            val scores = app.mindScoreRepository.getScoresBetween(userId, startDate, endDate)
            val scoreMap = scores.associate { it.date to it.score }

            val resultList = mutableListOf<Pair<String, Int>>()

            calendar.time = dateFormat.parse(startDate) ?: Date()

            for (i in 0..6) {
                val dateKey = dateFormat.format(calendar.time)
                val displayDate = displayFormat.format(calendar.time)
                val score = scoreMap[dateKey] ?: 0
                resultList.add(Pair(displayDate, score))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            result.postValue(resultList)
        }

        return result
    }

    private fun convertToDbDateFormat(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateStr) ?: Date()
            outputFormat.format(date)
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }


    private suspend fun hasAnyMindData(userId: Long, today: String): Boolean {

        if (app.journalRepository.getJournalEntryByDate(userId, today).first() != null) return true

        if (app.sleepRepository.getSleepLogsByUser(userId).first().any { it.date == today }) return true

        if (app.waterRepository.getWaterEntriesByUser(userId).first().any { it.date == today }) return true

        if (app.taskRepository.getTasksByUser(userId).first().any { it.date == today }) return true

        val (start, end) = todayMillisRange()

        if (app.workoutRepository.getWorkoutsByUser(userId).first().any { it.date in start..end }) return true

        val food = app.calorieRepository
            .getTodayFood(userId.toString(), LocalDate.now().toString())
            .first()

        if (food.isNotEmpty()) return true
        return false
    }




    private suspend fun computeEmotionalScore(userId: Long, today: String): Int {
        val entry = app.journalRepository.getJournalEntryByDate(userId, today).first() ?: return 0

        return when (entry.mood?.lowercase()) {
            "happy" -> 80
            "neutral" -> 50
            "sad" -> 20
            else -> 40
        }
    }

    private suspend fun computeSleepScore(userId: Long, today: String): Int {
        val logs =
            app.sleepRepository.getSleepLogsByUser(userId).first()
                .filter { it.date == today }

        if (logs.isEmpty()) return 0

        val latest = logs.last()

        var start = latest.startHour * 60 + latest.startMinute
        var end = latest.endHour * 60 + latest.endMinute

        if (end <= start) end += 24 * 60

        val hours = (end - start) / 60.0

        return when {
            hours >= 8 -> 100
            hours >= 7 -> 80
            hours >= 6 -> 60
            else -> 40
        }
    }

    private fun computeMeditationScore(userId: Long, today: String): Int {
        val prefs =
            getApplication<Application>()
                .getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)

        val json =
            prefs.getString("sessions_${userId.toInt()}", null) ?: return 0

        val type = object : TypeToken<MutableList<PastSession>>() {}.type

        val list: MutableList<PastSession> =
            Gson().fromJson(json, type) ?: return 0

        val todaySessions =
            list.filter { it.date == today }

        return when {
            todaySessions.size >= 3 -> 100
            todaySessions.size == 2 -> 70
            todaySessions.size == 1 -> 40
            else -> 0
        }
    }

    private suspend fun computeWaterScore(userId: Long, today: String): Int {
        val entries =
            app.waterRepository.getWaterEntriesByUser(userId).first()

        val settings = app.userSettingsRepository.getUserSettings(userId).first()
        val targetMl = settings?.waterTargetMl ?: 2000

        val todayMl =
            entries.filter { it.date == today }.sumOf { it.amountMl }

        if (todayMl == 0) return 0

        val ratio = todayMl.toDouble() / targetMl

        return when {
            ratio >= 1.2 -> 100
            ratio >= 1.0 -> 80
            ratio >= 0.75 -> 60
            ratio >= 0.5 -> 40
            else -> 0
        }
    }

    private suspend fun computeTaskScore(userId: Long, today: String): Int {
        val tasks =
            app.taskRepository.getTasksByUser(userId).first()
                .filter { it.date == today }

        if (tasks.isEmpty()) return 0

        val completed =
            tasks.count { it.completed }

        val pct = completed * 100 / tasks.size

        return pct
    }

    private suspend fun computePhysicalScore(userId: Long): Int {
        val workouts =
            app.workoutRepository.getWorkoutsByUser(userId).first()

        val todayWorkouts = workouts.filter { it.date in todayMillisRange().first..todayMillisRange().second }

        return when {
            todayWorkouts.size >= 2 -> 100
            todayWorkouts.size == 1 -> 60
            else -> 0
        }
    }


    private fun interpretScore(score: Int): String =
        when {
            score >= 90 -> "You're feeling great today! Keep the positive energy going 🌿"
            score >= 70 -> "Good day. You're doing well — stay consistent."
            score >= 50 -> "You're okay, but a short break or meditation might help."
            else -> "Your mind needs some care today. Take it slow and be kind to yourself."
        }

    private fun todayDateString(): String =
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

    private fun todayMillisRange(): Pair<Long, Long> {

        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val start = cal.timeInMillis
        val end = start + 86400000 - 1

        return Pair(start, end)
    }


    private fun formatSleepDuration(entity: SleepEntity): String {

        var start = entity.startHour * 60 + entity.startMinute
        var end = entity.endHour * 60 + entity.endMinute

        if (end <= start) end += 24 * 60

        val diff = end - start

        val h = diff / 60
        val m = diff % 60

        return "${h}h ${m}m"
    }

    private fun formatPeriodSummary(period: PeriodEntity?): String {

        if (period == null || period.startDate.isNullOrEmpty()) return "Not tracked"

        return try {

            val displayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            fun parseDate(dateStr: String?): Date? {
                if (dateStr.isNullOrEmpty()) return null

                val formats = listOf(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                )

                for (format in formats) {
                    try {
                        return format.parse(dateStr)
                    } catch (_: Exception) {}
                }
                return null
            }

            val startDate = parseDate(period.startDate) ?: return "Not tracked"
            val endDate = parseDate(period.endDate)

            val todayCal = Calendar.getInstance()
            val todayStart = todayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time


            if (endDate != null && !todayStart.before(startDate) && !todayStart.after(endDate)) {
                return "Ongoing"
            }


            val nextCal = Calendar.getInstance()
            nextCal.time = startDate
            nextCal.add(Calendar.DAY_OF_YEAR, period.cycleLength)

            val nextStart = nextCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val diffMillis = nextStart.time - todayStart.time
            val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            when {
                days < 0 -> "Next period on ${displayFormat.format(nextStart)}"
                days == 0 -> "Today"
                days == 1 -> "Tomorrow"
                days in 2..7 -> "In $days days"
                else -> "Next period on ${displayFormat.format(nextStart)}"
            }

        } catch (e: Exception) {
            "Not tracked"
        }
    }
    private fun startRealtimeSync() {

        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        app.taskRepository.startRealtimeSync(userId)
        app.waterRepository.startRealtimeSync(userId)
        app.workoutRepository.startRealtimeSync(userId)
        app.mindScoreRepository.startRealtimeSync(userId)
    }
}
