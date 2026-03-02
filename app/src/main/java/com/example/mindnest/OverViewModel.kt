package com.example.mindnest.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.mindnest.MindNestApplication
import com.example.mindnest.PastSession
import com.example.mindnest.data.entity.MindScoreEntity
import com.example.mindnest.data.entity.PeriodEntity
import com.example.mindnest.data.entity.SleepEntity
import com.example.mindnest.data.repository.MindScoreRepository
import com.example.mindnest.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlin.math.roundToInt
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
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

    private val _weeklyAverage = MutableLiveData<Int>()
    val weeklyAverage: LiveData<Int> = _weeklyAverage

    private val _weeklyMeta = MutableLiveData<String>()
    val weeklyMeta: LiveData<String> = _weeklyMeta

    private val _weeklyInsight = MutableLiveData<String>()
    val weeklyInsight: LiveData<String> = _weeklyInsight

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private var meditationListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {

            initUserId()

            val userId = preferenceManager.getUserId()
            if (userId <= 0) return@launch

            startRealtimeSync()
            startObservingModules()
            startObservingMindScore() // Start observing mind score flow
            refreshMeditation()
            refreshMindScore()
            startWeeklyObserver(userId)
        }
    }

    private suspend fun initUserId() {

        if (preferenceManager.getUserId() > 0) {
            _userName.postValue(preferenceManager.getUserName() ?: "User")
            return
        }

        val email = preferenceManager.getUserEmail()
        if (!email.isNullOrEmpty()) {

            val user = app.userRepository.getUserByEmail(email)

            if (user != null) {
                preferenceManager.saveUserId(user.id)
                preferenceManager.saveUserName(user.name)
                preferenceManager.saveUserEmail(user.email)
                preferenceManager.saveUserGender(user.gender)

                _userName.postValue(user.name)
            } else {
                _userName.postValue("User")
            }
        } else {
            _userName.postValue("User")
        }
    }

    private fun refreshWeeklyPerformance() {
        viewModelScope.launch {

            val userId = preferenceManager.getUserId()
            if (userId <= 0) {
                _weeklyAverage.postValue(0)
                _weeklyMeta.postValue("0/7 days • 0% consistency")
                _weeklyInsight.postValue("Start tracking your progress this week.")
                return@launch
            }

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val endDate = dateFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)

            val scores = withContext(Dispatchers.IO) {
                app.mindScoreRepository.getScoresBetween(userId, startDate, endDate)
            }

            val daysLogged = scores.size
            val totalScore = scores.sumOf { it.score }

            val average = if (daysLogged > 0) totalScore / daysLogged else 0
            val consistency = ((daysLogged / 7.0) * 100).toInt()

            _weeklyAverage.postValue(average)
            _weeklyMeta.postValue("$daysLogged/7 days • $consistency% consistency")
            _weeklyInsight.postValue(
                generateWeeklyInsight(average, daysLogged)
            )
        }
    }

    private fun generateWeeklyInsight(avg: Int, daysTracked: Int): String {

        if (daysTracked <= 2) {
            return when {
                avg >= 80 ->
                    "Strong start to the week 🌟 Keep this momentum going."
                avg >= 60 ->
                    "A steady beginning. Stay consistent and build from here."
                avg > 0 ->
                    "A gentle start. Small mindful steps today will shape your week."
                else ->
                    "Log your habits to start building this week’s emotional story."
            }
        }

        return when {
            avg >= 90 ->
                "This week felt aligned and powerful. You're showing real emotional strength 🌿"

            avg >= 80 ->
                "You stayed consistent and grounded. Small daily efforts are adding up beautifully."

            avg >= 70 ->
                "A strong week overall. Even on tougher days, you kept showing up."

            avg >= 60 ->
                "Some ups and downs, but you're navigating them with awareness."

            avg >= 50 ->
                "A mixed week. Consider a little more rest or mindful time for yourself."

            avg >= 35 ->
                "Energy felt a bit unstable this week. Gentle routines could help restore balance."

            avg > 0 ->
                "This week asked more from you. Slow down, reset, and prioritize yourself."

            else ->
                "Start tracking your days to unlock your weekly emotional story."
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
            app.journalRepository.getJournalEntryByDate(userId, todayDateString()).collect { entry ->
                _journalSummary.value =
                    if (entry == null) "Log today's mood"
                    else "${entry.mood?.replaceFirstChar { it.uppercase() } ?: "Mood"} mood logged"
            }
        }

        viewModelScope.launch {
            app.periodRepository.getPeriodTracking(userId).collect {
                _periodSummary.value = formatPeriodSummary(it)
            }
        }

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

        viewModelScope.launch {
            app.workoutRepository.getWorkoutsByUser(userId).collect { list ->
                val (start, end) = todayMillisRange()
                val todayWorkouts = list.filter { it.date in start..end }
                _workoutSummary.value = if (todayWorkouts.isEmpty()) "No workout" else "${todayWorkouts.size} workouts"
            }
        }


        viewModelScope.launch {
            val userIdStr = userId.toString()
            val today = LocalDate.now().toString()
            combine(
                app.calorieRepository.getTodayFood(userIdStr, today),
                app.calorieRepository.getUserFlow(userIdStr)
            ) { foodList, userInfo ->
                val total = foodList.sumOf { it.calories * it.quantity }
                val target = userInfo?.targetCalories ?: 2000
                "$total / $target kcal"
            }.collect { summary ->
                _calorieSummary.value = summary
                refreshMindScore()
            }
        }

        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseUid != null) {
            meditationListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUid)
                .collection("meditation_sessions")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val today = todayDateString()
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            val time = doc.getString("time") ?: return@mapNotNull null
                            val date = doc.getString("date") ?: return@mapNotNull null
                            val duration = doc.getString("duration") ?: return@mapNotNull null
                            val startMillis = doc.getLong("startMillis") ?: 0L
                            PastSession(time, date, duration, startMillis)
                        }

                        val count = sessions.count { it.date == today }
                        _meditationSummary.postValue("$count sessions")

                        val prefs = getApplication<Application>().getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)
                        prefs.edit().putString("sessions_$firebaseUid", Gson().toJson(sessions)).commit()

                        refreshMindScore()
                    }
                }
        }
    }

    private fun startObservingMindScore() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            val todayDbFormat = convertToDbDateFormat(todayDateString())
            app.mindScoreRepository.observeScoresBetween(userId, todayDbFormat, todayDbFormat).collect { scores ->
                val latestScore = scores.firstOrNull()?.score
                if (latestScore != null && _mindScore.value != latestScore) {
                    _mindScore.postValue(latestScore)
                    _mindScoreStatus.postValue(interpretScore(latestScore))
                }
            }
        }
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
        val prefs = getApplication<Application>().getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return "0 sessions"
        val json = prefs.getString("sessions_$firebaseUid", null) ?: return "0 sessions"
        val type = object : TypeToken<MutableList<PastSession>>() {}.type
        val list: MutableList<PastSession> = Gson().fromJson(json, type) ?: mutableListOf()
        val today = todayDateString()
        val count = list.count { it.date == today }
        return "$count sessions"
    }

    private fun refreshCalories() {
        viewModelScope.launch {
            val userId = preferenceManager.getUserId().toString()
            val today = LocalDate.now().toString()
            val list = app.calorieRepository.getTodayFood(userId, today).first()
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
            val calories = computeCalorieScore(userId)

            val score = (
                    emotional * 0.10 +
                            sleep * 0.16 +
                            meditation * 0.15 +
                            water * 0.14 +
                            physical * 0.15 +
                            calories * 0.15 +
                            tasks * 0.15
                    ).roundToInt()
            val finalScore = score.coerceIn(0, 100)

            _mindScore.postValue(finalScore)
            _mindScoreStatus.postValue(interpretScore(finalScore))

            val todayDbFormat = convertToDbDateFormat(today)

            withContext(Dispatchers.IO) {
                val existing = app.mindScoreRepository.getScoreByDate(userId, todayDbFormat)
                if (existing != finalScore) {
                    app.mindScoreRepository.insertScore(MindScoreEntity(userId = userId, date = todayDbFormat, score = finalScore))
                }
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
        val food = app.calorieRepository.getTodayFood(userId.toString(), LocalDate.now().toString()).first()
        if (food.isNotEmpty()) return true

        val prefs = getApplication<Application>().getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseUid != null) {
            val json = json(prefs.getString("sessions_$firebaseUid", null))
            if (json != null) {
                val type = object : TypeToken<List<PastSession>>() {}.type
                val list: List<PastSession> = Gson().fromJson(json, type) ?: emptyList()
                if (list.any { it.date == today }) return true
            }
        }

        return false
    }

    private fun json(str: String?): String? = str

    private val moodScoreMap = mapOf(
        "😊" to 100,
        "🙂" to 80,
        "😔" to 40
    )

    private suspend fun computeEmotionalScore(userId: Long, today: String): Int {

        val entries = app.journalRepository
            .getAllJournalEntriesByDate(userId, today)
            .first()

        if (entries.isEmpty()) return 0

        val moodScores = entries.map { entry ->
            moodScoreMap[entry.mood] ?: 50
        }

        return moodScores.average().toInt()
    }

    private suspend fun computeSleepScore(userId: Long, today: String): Int {

        val logs = app.sleepRepository
            .getSleepLogsByUser(userId)
            .first()
            .filter { it.date == today }

        if (logs.isEmpty()) return 0

        var totalMinutes = 0

        logs.forEach { entity ->
            var start = entity.startHour * 60 + entity.startMinute
            var end = entity.endHour * 60 + entity.endMinute

            if (end <= start) end += 24 * 60

            totalMinutes += (end - start)
        }

        val hours = totalMinutes / 60.0

        return when {
            hours >= 8 -> 100
            hours >= 7 -> 80
            hours >= 6 -> 60
            else -> 40
        }
    }

    private fun computeMeditationScore(userId: Long, today: String): Int {

        val prefs = getApplication<Application>()
            .getSharedPreferences("mindful_sessions", Context.MODE_PRIVATE)

        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0
        val json = prefs.getString("sessions_$firebaseUid", null) ?: return 0

        val type = object : TypeToken<MutableList<PastSession>>() {}.type
        val list: MutableList<PastSession> = Gson().fromJson(json, type) ?: return 0

        val (start, end) = todayMillisRange()

        val todaySessions = list.filter {
            it.startMillis in start..end
        }

        val totalMinutes = todaySessions.sumOf { session ->
            session.duration
                .replace("[^0-9]".toRegex(), "")
                .toIntOrNull() ?: 0
        }

        return when {
            totalMinutes >= 10 -> 100
            totalMinutes > 0 -> ((totalMinutes / 10.0) * 100).toInt()
            else -> 0
        }
    }

    private suspend fun computeWaterScore(userId: Long, today: String): Int {
        val entries = app.waterRepository.getWaterEntriesByUser(userId).first()
        val settings = app.userSettingsRepository.getUserSettings(userId).first()
        val targetMl = settings?.waterTargetMl ?: 2000
        val todayMl = entries.filter { it.date == today }.sumOf { it.amountMl }
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

    private suspend fun computeCalorieScore(userId: Long): Int {

        val today = LocalDate.now().toString()

        val foodList = app.calorieRepository
            .getTodayFood(userId.toString(), today)
            .first()

        if (foodList.isEmpty()) return 0

        val totalCalories = foodList.sumOf { it.calories * it.quantity }

        val userInfo = app.calorieRepository.getUser(userId.toString())
        val targetCalories = userInfo?.targetCalories ?: 2000

        if (targetCalories <= 0) return 0

        val percentage = (totalCalories.toDouble() / targetCalories) * 100

        return when {
            percentage in 95.0..105.0 -> 100
            percentage in 85.0..94.9 -> 80
            percentage in 70.0..84.9 -> 60
            percentage > 105.0 && percentage <= 120.0 -> 70
            percentage > 120.0 -> 50
            percentage in 50.0..69.9 -> 40
            else -> 0
        }
    }
    private suspend fun computeTaskScore(userId: Long, today: String): Int {
        val tasks = app.taskRepository.getTasksByUser(userId).first().filter { it.date == today }
        if (tasks.isEmpty()) return 0
        val completed = tasks.count { it.completed }
        return completed * 100 / tasks.size
    }

    private suspend fun computePhysicalScore(userId: Long): Int {
        val workouts = app.workoutRepository.getWorkoutsByUser(userId).first()
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

    private fun todayDateString(): String = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

    private fun todayMillisRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 86400000 - 1
        return start to end
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
                    try { return format.parse(dateStr) } catch (_: Exception) {}
                }
                return null
            }
            val startDate = parseDate(period.startDate) ?: return "Not tracked"
            val endDate = parseDate(period.endDate)
            val todayCal = Calendar.getInstance()
            val todayStart = todayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            if (endDate != null && !todayStart.before(startDate) && !todayStart.after(endDate)) return "Ongoing"
            val nextCal = Calendar.getInstance()
            nextCal.time = startDate
            nextCal.add(Calendar.DAY_OF_YEAR, period.cycleLength)
            val nextStart = nextCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
            val days = ((nextStart.time - todayStart.time) / (1000 * 60 * 60 * 24)).toInt()
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
        app.journalRepository.startRealtimeSync(userId)
        app.sleepRepository.startRealtimeSync(userId)
        app.periodRepository.startRealtimeSync(userId)
        app.calorieRepository.startUserRealtimeSync(userId.toString())
        app.calorieRepository.startFoodRealtimeSync(userId.toString())
    }

    fun startWeeklyObserver(userId: Long) {

        viewModelScope.launch {

            val (startDate, endDate) = getCurrentWeekRange()

            app.mindScoreRepository
                .observeScoresBetween(userId, startDate, endDate)
                .collect { scores ->

                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date())

                    val filtered = scores.filter { it.date <= today }

                    val daysPassed = getDaysPassedInWeek()
                    val daysLogged = filtered.size

                    val totalScore = filtered.sumOf { it.score }
                    val average = if (daysLogged > 0) totalScore / daysLogged else 0

                    val consistency = ((daysLogged / daysPassed.toDouble()) * 100).toInt()

                    _weeklyAverage.value = average
                    _weeklyMeta.value =
                        "$daysLogged/$daysPassed days • $consistency% consistency"
                    _weeklyInsight.postValue(
                        generateWeeklyInsight(average, daysLogged)
                    )
                }
        }
    }

    private fun getDaysPassedInWeek(): Int {

        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)

        return when (today) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 7
        }
    }

    private fun getCurrentWeekRange(): Pair<String, String> {

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val monday = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val sunday = calendar.time

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return format.format(monday) to format.format(sunday)
    }

    override fun onCleared() {
        super.onCleared()
        meditationListener?.remove()
    }
}