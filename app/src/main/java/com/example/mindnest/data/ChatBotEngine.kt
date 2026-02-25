package com.example.mindnest.data

import android.content.Context
import java.util.*
import kotlin.math.abs
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
    val pastWeekWater: List<Int> = emptyList(),
    val pastWeekSleep: List<Double> = emptyList()
) {

    fun hasWaterData() = waterSummary.isNotEmpty() && waterSummary != "Set target" && waterSummary != "Loading…"
    fun hasSleepData() = sleepSummary.isNotEmpty() && sleepSummary != "No logs today" && sleepSummary != "Loading…"
    fun hasWorkoutData() = workoutSummary.isNotEmpty() && workoutSummary != "No workout" && workoutSummary != "Loading…"
    fun hasMoodData() = journalSummary.isNotEmpty() && journalSummary != "Log today's mood" && journalSummary != "Loading…"
    fun hasTaskData() = taskSummary.isNotEmpty() && taskSummary != "No tasks" && taskSummary != "Loading…"
    fun hasMeditationData() = meditationSummary.isNotEmpty() && meditationSummary != "0 sessions" && meditationSummary != "Loading…"
    fun hasCalorieData() = calorieSummary.isNotEmpty() && calorieSummary != "Loading…"
    fun hasValidMindScore() = mindScore in 1..100
}

data class ChatMemory(
    var lastIntent: String? = null,
    var lastAdvice: String = "",
    val history: MutableList<String> = mutableListOf()
)

object ChatBotEngine {

    private val random = Random

    fun getReply(
        message: String,
        context: Context,
        ctx: ChatBotContext,
        memory: ChatMemory = ChatMemory()
    ): String {

        val msg = message.lowercase(Locale.getDefault()).trim()
        if (msg.isBlank()) return "Tell me what's on your mind 🌿"

        memory.history.add("U:$msg")

        val intent = detectIntent(msg)
        memory.lastIntent = intent

        val reply = buildReply(intent, ctx, memory)

        memory.history.add("B:$reply")

        return reply
    }


    private fun detectIntent(msg: String): String {

        return when {

            msg.containsAny("hi","hello","hey") -> "greeting"
            msg.containsAny("score","mind") -> "score"
            msg.containsAny("water","drink") -> "water"
            msg.containsAny("sleep","tired") -> "sleep"
            msg.containsAny("stress","anxious","overwhelmed") -> "stress"
            msg.containsAny("workout","exercise") -> "workout"
            msg.containsAny("mood","feel") -> "mood"
            msg.containsAny("summary","overview") -> "summary"
            msg.containsAny("suggest","advice") -> "advice"
            msg.containsAny("motivate","motivation") -> "motivation"

            else -> "unknown"
        }
    }

    private fun String.containsAny(vararg words: String): Boolean {
        return words.any { this.contains(it) }
    }


    private fun buildReply(intent: String, ctx: ChatBotContext, memory: ChatMemory): String {

        val analysis = buildWeeklyAnalysis(ctx)
        val burnout = burnoutCheck(ctx)
        val habit = habitInsights(ctx)
        val period = periodInsights(ctx)

        val core = when(intent) {

            "greeting" -> greeting(ctx)

            "score" -> mindScore(ctx)

            "water" -> water(ctx)

            "sleep" -> sleep(ctx)

            "stress" -> stressSupport()

            "workout" -> workout(ctx)

            "mood" -> mood(ctx)

            "summary" -> summary(ctx)

            "advice" -> suggestions(ctx)

            "motivation" -> motivation()

            else -> fallback(ctx)
        }

        return listOf(core, analysis, burnout, habit, period)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }



    private fun greeting(ctx: ChatBotContext): String {

        val name = ctx.userName.ifBlank { "there" }

        return "Hey $name 👋\n\n${priorityInsight(ctx)}"
    }


    private fun priorityInsight(ctx: ChatBotContext): String {

        return when {

            !ctx.hasSleepData() ->
                "Logging your sleep could improve your energy today."

            !ctx.hasWaterData() ->
                "Hydration looks low. Drinking water now could help."

            !ctx.hasMoodData() ->
                "A quick mood check-in may help emotional clarity."

            else ->
                "You're doing well — consistency is key 🌿"
        }
    }




    private fun mindScore(ctx: ChatBotContext): String {

        if (!ctx.hasValidMindScore())
            return "I don’t have enough data yet. Try logging sleep or water today."

        return """
            Your MindScore is ${ctx.mindScore}/100 🌿
            
            ${ctx.mindScoreStatus}
        """.trimIndent()
    }


    private fun water(ctx: ChatBotContext) =
        if (ctx.hasWaterData()) "Hydration today: ${ctx.waterSummary}"
        else "You haven’t logged water yet."

    private fun sleep(ctx: ChatBotContext) =
        if (ctx.hasSleepData()) "Sleep: ${ctx.sleepSummary}"
        else "No sleep logged yet."

    private fun workout(ctx: ChatBotContext) =
        if (ctx.hasWorkoutData()) "Workout: ${ctx.workoutSummary}"
        else "No workout logged."

    private fun mood(ctx: ChatBotContext) =
        if (ctx.hasMoodData()) "Mood: ${ctx.journalSummary}"
        else "Mood not logged yet."



    private fun stressSupport(): String {

        val tips = listOf(
            "Try slow breathing: inhale 4s, exhale 6s.",
            "Relax shoulders and unclench jaw.",
            "Step away for 2 minutes."
        )

        return "I’m here with you 💙\n\n${tips.random()}"
    }


    private fun summary(ctx: ChatBotContext): String {

        val parts = mutableListOf<String>()

        if (ctx.hasValidMindScore()) parts.add("MindScore: ${ctx.mindScore}")
        if (ctx.hasWaterData()) parts.add("Water: ${ctx.waterSummary}")
        if (ctx.hasSleepData()) parts.add("Sleep: ${ctx.sleepSummary}")
        if (ctx.hasWorkoutData()) parts.add("Workout: ${ctx.workoutSummary}")
        if (ctx.hasMoodData()) parts.add("Mood: ${ctx.journalSummary}")

        return "Here’s your day snapshot:\n\n${parts.joinToString("\n")}"
    }


    private fun suggestions(ctx: ChatBotContext): String {

        val actions = mutableListOf<String>()

        if (!ctx.hasWaterData()) actions.add("Drink a glass of water")
        if (!ctx.hasSleepData()) actions.add("Log your sleep")
        if (!ctx.hasMoodData()) actions.add("Log yourr mood")
        if (!ctx.hasWorkoutData()) actions.add("Take a short walk")

        if (actions.isEmpty())
            return "You’re doing great today 🌿"

        return "Suggestions:\n\n" + actions.joinToString("\n") { "• $it" }
    }



    private fun motivation(): String {

        val quotes = listOf(
            "Small steps still move you forward.",
            "Consistency beats perfection.",
            "You’re stronger than you think.",
            "Progress is progress."
        )

        return quotes.random() + " 🌿"
    }



    private fun buildWeeklyAnalysis(ctx: ChatBotContext): String {

        if (ctx.pastWeekWater.isEmpty() || ctx.pastWeekSleep.isEmpty()) return ""

        val avgWater = ctx.pastWeekWater.average()
        val avgSleep = ctx.pastWeekSleep.average()

        return when {

            avgSleep < 6 ->
                "Your sleep trend this week looks low. Improving sleep could boost your MindScore."

            avgWater < 1500 ->
                "Hydration trend is below optimal this week."

            else ->
                "Your weekly habits look stable 👍"
        }
    }


    private fun burnoutCheck(ctx: ChatBotContext): String {

        if (!ctx.hasValidMindScore()) return ""

        val risk =
            (ctx.mindScore < 40) &&
                    (!ctx.hasSleepData()) &&
                    (!ctx.hasMeditationData())

        return if (risk)
            "You might be experiencing mental fatigue. Prioritize rest and recovery today 💙"
        else ""
    }


    private fun habitInsights(ctx: ChatBotContext): String {

        val scores = mutableMapOf<String, Int>()

        if (ctx.hasSleepData()) scores["Sleep"] = 1
        if (ctx.hasWaterData()) scores["Hydration"] = 1
        if (ctx.hasWorkoutData()) scores["Activity"] = 1
        if (ctx.hasMeditationData()) scores["Mindfulness"] = 1

        if (scores.isEmpty()) return ""

        val best = scores.keys.random()

        return "Your strongest habit right now seems to be: $best 👍"
    }



    private fun periodInsights(ctx: ChatBotContext): String {

        if (ctx.periodSummary.contains("Today", true))
            return "You may experience lower energy today. Gentle self-care is helpful."

        if (ctx.periodSummary.contains("Tomorrow", true))
            return "Your cycle is approaching. Prioritize rest and hydration."

        return ""
    }



    private fun fallback(ctx: ChatBotContext): String {

        return "I’m here to support your wellness 🌿\n\n${priorityInsight(ctx)}"
    }

    fun typingDelay(text: String): Long {
        return (text.length * 18L).coerceIn(600L, 2500L)
    }
}