package com.example.mindnest.data

import android.content.Context
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

data class ChatBotContext(
    val userName: String,
    val mindScore: Int,
    val mindScoreStatus: String,
    val taskSummary: String,
    val waterSummary: String,
    val sleepSummary: String,
    val workoutSummary: String,
    val journalSummary: String,
    val periodSummary: String,
    val calorieSummary: String,
    val meditationSummary: String,
    val weeklyAverage: Int = 0,
    val weeklyConsistency: String = "",
    val weeklyInsightText: String = "",
    val pastWeekWater: List<Int> = emptyList(),
    val pastWeekSleep: List<Double> = emptyList(),
    val pastWeekMindScore: List<Int> = emptyList()
) {
    fun hasWaterData() = waterSummary.isMeaningful()
    fun hasSleepData() = sleepSummary.isMeaningful()
    fun hasWorkoutData() = workoutSummary.isMeaningful()
    fun hasMoodData() = journalSummary.isMeaningful()
    fun hasTaskData() = taskSummary.isMeaningful()
    fun hasMeditationData() = meditationSummary.isMeaningful()
    fun hasCalorieData() = calorieSummary.isMeaningful()
    fun hasValidMindScore() = mindScore in 1..100

    private fun String.isMeaningful(): Boolean {
        val invalid = listOf("Loading", "No", "Set", "0 sessions", "Log")
        return this.isNotBlank() && invalid.none { this.contains(it, true) }
    }
}

data class ChatMemory(
    var lastIntent: String? = null,
    var emotionalTone: String = "neutral",
    var lastAdvice: String = "",
    val history: MutableList<String> = mutableListOf()
)

object ChatBotEngine {

    private val random = Random

    private val intentLibrary = mapOf(
        "greeting" to listOf("hi","hello","hey","good morning","good afternoon","good evening","good night"),
        "workout" to listOf("workout","exercise","gym","training","fitness","physical","activity"),
        "sleep" to listOf("sleep","rest","tired","bed","insomnia"),
        "water" to listOf("water","hydration","drink"),
        "calorie" to listOf("calorie","food","eat","diet","nutrition"),
        "period" to listOf("period","cycle","menstrual","pms"),
        "mindscore" to listOf("mindscore","mental score","performance"),
        "summary" to listOf("summary","overview","report","everything","full report"),
        "motivation" to listOf("motivate","encourage","push","discipline","inspire"),
        "advice" to listOf("improve","suggest","guide","better"),
        "stress" to listOf("stress","anxious","overwhelmed","burnout","pressure"),
        "mood" to listOf("mood","feel","emotion","sad","happy","angry","low")
    )

    private val expandedIntentLibrary = mapOf(
        "meditation" to listOf("meditation","breathing","mindfulness","calm","relax"),
        "task" to listOf("task","todo","productivity","pending","completed"),
        "calorie_detail" to listOf("protein","carbs","fat","macros"),
        "period_detail" to listOf("ovulation","cycle day","flow","cramps"),
        "water_detail" to listOf("litre","liters","ml","dehydrated"),
        "sleep_detail" to listOf("deep sleep","rem","sleep quality"),
        "motivation_strong" to listOf("push me hard","be strict","discipline mode"),
        "general_ai" to listOf("talk to me","chat","conversation","random"),
        "weekly_performance" to listOf("weekly performance","weekly score","week summary","progress this week")
    )

    fun getReply(
        message: String,
        context: Context,
        ctx: ChatBotContext,
        memory: ChatMemory = ChatMemory()
    ): String {

        val clean = message.trim()
        if (clean.isBlank()) return "Tell me what’s on your mind 🌿"

        memory.history.add("U:$clean")

        val intent = detectIntent(clean.lowercase(Locale.getDefault()), memory)
        memory.lastIntent = intent

        val core = buildReply(intent, clean, ctx, memory)
        val adaptive = adaptiveInsights(ctx)
        val weekly = weeklyInsights(ctx)

        return listOf(core, adaptive, weekly)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .also { memory.history.add("B:$it") }
    }

    private fun detectIntent(message: String, memory: ChatMemory): String {
        var bestIntent = "general"
        var bestScore = 0
        (intentLibrary + expandedIntentLibrary).forEach { (intent, keywords) ->
            val score = keywords.count { message.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestIntent = intent
            }
        }
        return if (bestScore > 0) bestIntent else memory.lastIntent ?: "general"
    }

    private fun buildReply(
        intent: String,
        raw: String,
        ctx: ChatBotContext,
        memory: ChatMemory
    ): String {

        val core = when (intent) {

            "greeting" -> greeting(ctx)
            "workout" -> workout(ctx)
            "sleep" -> sleep(ctx)
            "water" -> water(ctx)
            "calorie" -> calorie(ctx)
            "period" -> period(ctx)
            "mindscore" -> mindScore(ctx)
            "summary" -> summary(ctx)
            "motivation" -> motivation(ctx)
            "motivation_strong" -> strongMotivation()
            "advice" -> advice(ctx)
            "stress" -> stressSupport(memory)
            "mood" -> moodReflection(ctx, memory)
            "meditation" -> meditationAdvanced(ctx)
            "task" -> taskAdvanced(ctx)
            "weekly_performance" -> weeklyPerformance(ctx)
            else -> normalConversation(raw)
        }

        val adaptive = adaptiveInsights(ctx)
        val weekly = weeklyInsights(ctx)

        return listOf(core, adaptive, weekly)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun greeting(ctx: ChatBotContext): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning ${ctx.userName} ☀️ Let’s build momentum today."
            hour < 17 -> "Good afternoon ${ctx.userName} 🌿 Stay steady and intentional."
            hour < 21 -> "Good evening ${ctx.userName} 🌙 Reflect on your effort."
            else -> goodNight(ctx)
        }
    }

    private fun goodNight(ctx: ChatBotContext): String = when {
        !ctx.hasValidMindScore() -> "Good night 🌙 Rest and recharge."
        ctx.mindScore >= 85 -> "Outstanding day 🌙 You performed at an elite level."
        ctx.mindScore >= 70 -> "Strong disciplined day 🌙 Keep compounding success."
        ctx.mindScore >= 50 -> "Decent progress 🌙 Reflect and refine tomorrow."
        else -> "Tough day 🌙 Rest without guilt. Reset and rise tomorrow."
    }

    private fun workout(ctx: ChatBotContext): String {
        if (!ctx.hasWorkoutData()) return "No workout logged. Even 20 minutes improves clarity."
        val evaluation = when {
            ctx.workoutSummary.contains("60") -> "Excellent endurance and intensity."
            ctx.workoutSummary.contains("45") -> "Strong structured session."
            ctx.workoutSummary.contains("30") -> "Good baseline activity."
            else -> "Movement recorded. Try progressive overload."
        }
        return "Workout Summary:\n${ctx.workoutSummary}\n\n$evaluation"
    }

    private fun sleep(ctx: ChatBotContext): String {
        if (!ctx.hasSleepData()) return "Sleep not logged."
        val weeklyAvg = if (ctx.pastWeekSleep.isNotEmpty()) ctx.pastWeekSleep.average().roundToInt() else null
        return buildString {
            append("Sleep Summary:\n${ctx.sleepSummary}")
            if (weeklyAvg != null) append("\nWeekly Avg: $weeklyAvg hrs.")
            append("\nSleep regulates mood, memory and recovery.")
        }
    }

    private fun water(ctx: ChatBotContext): String {
        if (!ctx.hasWaterData()) return "Hydration not logged."
        return "Hydration Summary:\n${ctx.waterSummary}\nHydration boosts cognition and metabolism."
    }

    private fun calorie(ctx: ChatBotContext): String {
        if (!ctx.hasCalorieData()) return "Nutrition not tracked."
        return """
Nutrition Summary:
${ctx.calorieSummary}

Insight:
• Prioritize protein (1.2–1.6g/kg bodyweight)
• Avoid large late-night sugar spikes
• Balance carbs around workouts
        """.trimIndent()
    }

    private fun period(ctx: ChatBotContext): String {
        if (ctx.periodSummary.isBlank()) return "Cycle data unavailable."
        return """
Cycle Update:
${ctx.periodSummary}

Recommendations:
• Follicular → Best for high intensity workouts
• Luteal → Focus on recovery and magnesium
• Menstrual → Light movement + iron rich foods
        """.trimIndent()
    }

    private fun meditationAdvanced(ctx: ChatBotContext): String {
        if (!ctx.hasMeditationData()) return "Meditation not logged yet."
        return """
Meditation Summary:
${ctx.meditationSummary}

Meditation improves:
• Emotional regulation
• Focus stability
• Stress response control

Aim for 10–15 minutes daily consistency.
        """.trimIndent()
    }

    private fun taskAdvanced(ctx: ChatBotContext): String {
        if (!ctx.hasTaskData()) return "No tasks logged."
        return """
Task Progress:
${ctx.taskSummary}

Suggestion:
• Tackle high-value tasks first
• Apply 90-minute deep work blocks
• Reduce distractions during focus time
        """.trimIndent()
    }

    private fun mindScore(ctx: ChatBotContext): String {
        if (!ctx.hasValidMindScore()) return "MindScore unavailable."
        return "MindScore: ${ctx.mindScore}/100\n${ctx.mindScoreStatus}"
    }

    private fun summary(ctx: ChatBotContext): String {
        return """
Full Overview:

MindScore: ${ctx.mindScore}
Sleep: ${ctx.sleepSummary}
Hydration: ${ctx.waterSummary}
Workout: ${ctx.workoutSummary}
Calories: ${ctx.calorieSummary}
Mood: ${ctx.journalSummary}
Meditation: ${ctx.meditationSummary}
Tasks: ${ctx.taskSummary}
        """.trimIndent()
    }

    private fun motivation(ctx: ChatBotContext): String {
        val messages = listOf(
            "Consistency creates confidence.",
            "Small progress compounds massively.",
            "Discipline builds identity.",
            "Action reduces anxiety.",
            "Show up especially when it’s hard."
        )
        return messages.random()
    }

    private fun strongMotivation(): String = """
No excuses.

You don’t need motivation.
You need discipline.

Do the work.
Stack the wins.
Build the identity.
        """.trimIndent()

    private fun advice(ctx: ChatBotContext): String {
        val tips = mutableListOf<String>()
        if (!ctx.hasSleepData()) tips.add("Improve sleep timing and consistency.")
        if (!ctx.hasWaterData()) tips.add("Increase hydration.")
        if (!ctx.hasWorkoutData()) tips.add("Add structured exercise.")
        if (!ctx.hasMeditationData()) tips.add("Include short meditation.")
        return if (tips.isEmpty()) "You are maintaining balanced habits." else "Focus Areas:\n${tips.joinToString("\n")}"
    }

    private fun stressSupport(memory: ChatMemory): String {
        memory.emotionalTone = "supportive"
        val responses = listOf(
            "Pause. Take a slow breath.",
            "Stress is feedback, not failure.",
            "Focus only on the next small step.",
            "You are not behind. You are learning.",
            "Regulate first. Solve second."
        )
        return responses.random()
    }

    private fun moodReflection(ctx: ChatBotContext, memory: ChatMemory): String {
        memory.emotionalTone = "empathetic"
        if (!ctx.hasMoodData()) return "You haven’t logged mood today."
        return "Mood Reflection:\n${ctx.journalSummary}\nAwareness builds control."
    }

    private fun normalConversation(input: String): String = advancedConversation()

    private fun advancedConversation(): String {
        val responses = listOf(
            "Tell me more about that.",
            "What’s your biggest focus right now?",
            "What would progress look like?",
            "Interesting perspective. Continue.",
            "How are you feeling about that?",
            "Let’s break that into steps.",
            "What outcome are you aiming for?",
            "That matters. Expand on it.",
            "What’s stopping you?",
            "What would your best self do here?",
            "If today improved by 1%, what would change?",
            "What’s one action you can take now?",
            "Clarity reduces stress. Let’s clarify."
        )
        return responses.random()
    }

    private fun adaptiveInsights(ctx: ChatBotContext): String {
        if (!ctx.hasValidMindScore()) return ""
        return when {
            ctx.mindScore > 90 -> "You are operating in elite cognitive range."
            ctx.mindScore < 40 -> "Prioritize recovery, sleep and low-intensity day."
            else -> ""
        }
    }

    private fun weeklyInsights(ctx: ChatBotContext): String {
        val insights = mutableListOf<String>()
        if (ctx.weeklyAverage > 0) insights.add("This week your average MindScore is ${ctx.weeklyAverage}/100.")
        if (ctx.weeklyInsightText.isNotBlank()) insights.add(ctx.weeklyInsightText)
        if (ctx.pastWeekSleep.isNotEmpty()) {
            val avgSleep = ctx.pastWeekSleep.average()
            if (avgSleep < 6) insights.add("Weekly sleep average is below optimal.")
        }
        if (ctx.pastWeekWater.isNotEmpty()) {
            val avgWater = ctx.pastWeekWater.average()
            if (avgWater < 2000) insights.add("Weekly hydration could improve.")
        }
        if (ctx.pastWeekMindScore.isNotEmpty()) {
            val trend = ctx.pastWeekMindScore.joinToString(", ")
            insights.add("MindScore trend: $trend")
        }
        return insights.joinToString("\n")
    }

    private fun weeklyPerformance(ctx: ChatBotContext): String {
        if (ctx.weeklyAverage == 0) return "Weekly performance not available yet."
        return """
Weekly Performance:
Average MindScore: ${ctx.weeklyAverage}/100
Consistency: ${ctx.weeklyConsistency}
Insight: ${ctx.weeklyInsightText}
        """.trimIndent()
    }

    fun typingDelay(text: String): Long {
        return (text.length * 15L).coerceIn(700L, 2800L)
    }
}