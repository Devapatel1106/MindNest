package com.example.mindnest.data

import android.content.Context
import java.util.*
import kotlin.math.min
import kotlin.random.Random
import kotlin.math.abs

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
    var lastSuggestion: String = "",
    var lastIntent: String? = null,
    val conversationHistory: MutableList<String> = mutableListOf()
)

object ChatBotEngine {

    private val random = Random

    private val intentKeywords = mapOf(
        "greeting" to listOf("hi","hello","hey","hiya","howdy"),
        "mindScore" to listOf("mindscore","mind score","score","wellness score","how am i doing"),
        "water" to listOf("water","hydration","drink","glasses","intake","hydrate"),
        "sleep" to listOf("sleep","slept","tired","rest","insomnia","nap"),
        "workout" to listOf("workout","exercise","fitness","gym","run","walk","activity"),
        "mood" to listOf("mood","journal","feeling","emotion","how i feel"),
        "period" to listOf("period","cycle","pms","menstrual"),
        "calories" to listOf("calorie","calories","food","eating","diet"),
        "meditation" to listOf("meditation","meditate","mindfulness","breathe","calm"),
        "tasks" to listOf("task","tasks","todo","to-do","done","productivity"),
        "stress" to listOf("stress","stressed","anxiety","anxious","overwhelmed","panic","worried"),
        "motivation" to listOf("motivat","encourage","quote","inspire"),
        "summary" to listOf("summary","overview","insight","how am i","today","my day"),
        "help" to listOf("help","what can you","what do you do","capabilities"),
        "suggestion" to listOf("suggest","recommend","what should","what can i do","tip","advice")
    )

    private val sentenceReplies = mapOf(
        "can you motivate me for workout" to "Absolutely! Even 10 minutes of movement can boost your mood and focus. Start small and you'll see progress.",
        "i feel lazy to workout" to "It’s normal to feel that way. Try just 5 minutes of stretching or a short walk—you’ll feel energized after.",
        "suggest a workout for today" to "How about a brisk walk, 10 push-ups, or 2 minutes of jump rope? Small steps count!",
        "how much water should i drink today" to "Aim for at least 8 glasses (around 2 liters). Hydration supports mood, focus, and energy.",
        "i forgot to drink water" to "No worries! Take a glass now and try to sip water regularly through the day.",
        "i couldn't sleep well" to "A calm bedtime routine helps. Try dim lights, avoid screens, and take deep breaths before sleeping.",
        "how many hours should i sleep" to "Aim for 7-9 hours of quality sleep. Consistency is key for energy and mood.",
        "i feel stressed today" to "I hear you. Try 4-4-6 breathing or a quick meditation to calm your mind.",
        "i am feeling happy" to "That’s wonderful! Take a moment to celebrate the good vibes.",
        "what is my mindscore today" to "Your MindScore helps track your wellness. Check logs like sleep, water, and mood for an accurate score.",
        "how can i improve my mindscore" to "Focus on small wins: drink water, sleep well, log your mood, and do a short meditation.",
        "i want to meditate" to "Great! Even 2 minutes of mindful breathing can reduce stress and improve focus.",
        "suggest a meditation exercise" to "Try 2-minutes deep breathing: inhale 4s, hold 4s, exhale 6s, repeat 5 times.",
        "i have a lot of tasks" to "Break them into 1-2 small tasks first. Completing even a few boosts motivation.",
        "what should i do first" to "Prioritize tasks that are urgent and easy to start. Momentum matters!",
        "when is my next period" to "Check your period log to see predicted dates. Logging consistently improves accuracy.",
        "how is my cycle today" to "Your period summary gives insights. Make sure to log symptoms and flow.",
        "how many calories did i eat today" to "Check your calorie summary to see your intake. Logging meals helps track nutrition.",
        "suggest a healthy meal" to "Include a balance of protein, carbs, and veggies. Example: grilled chicken with quinoa and salad.",
        "motivate me" to "You’ve got this! Start with one small habit today. Progress adds up over time.",
        "i need encouragement" to "Remember, showing up for yourself is already a win. Keep going 🌿"
    )

    fun getReply(message: String, context: Context, ctx: ChatBotContext, memory: ChatMemory = ChatMemory()): String {
        val msg = message.trim().lowercase(Locale.getDefault())
        if (msg.isEmpty()) return "I didn’t catch that. How can I help you today?"

        memory.conversationHistory.add("User: $message")

        getSentenceBasedReply(msg)?.let { reply ->
            memory.conversationHistory.add("Bot: $reply")
            return reply
        }

        val intents = getIntent(msg)
        val proactive = getProactiveSuggestions(ctx, memory)

        if (intents.isEmpty()) return proactive.ifEmpty { replyFallback(ctx, memory) }

        val replies = intents.map { intent ->
            memory.lastIntent = intent
            when (intent) {
                "greeting" -> replyGreeting(ctx)
                "mindScore" -> replyMindScore(ctx)
                "water" -> replyWater(ctx)
                "sleep" -> replySleep(ctx)
                "workout" -> replyWorkout(ctx)
                "mood" -> replyMood(ctx)
                "period" -> replyPeriod(ctx)
                "calories" -> replyCalories(ctx)
                "meditation" -> replyMeditation(ctx)
                "tasks" -> replyTasks(ctx)
                "stress" -> replyStress(ctx)
                "motivation" -> replyMotivation(ctx)
                "summary" -> replyPersonalisedSummary(ctx)
                "help" -> replyHelp()
                "suggestion" -> replySuggestions(ctx)
                else -> null
            }
        }.filterNotNull()

        val finalReply = listOf(proactive, replies.joinToString("\n\n")).filter { it.isNotBlank() }.joinToString("\n\n")
        memory.conversationHistory.add("Bot: $finalReply")
        return finalReply
    }


    private fun getIntent(msg: String): List<String> {
        val matchedIntents = mutableListOf<String>()
        for ((intent, keywords) in intentKeywords) {
            for (kw in keywords) {
                if (similarity(msg, kw) > 0.7) {
                    matchedIntents.add(intent)
                    break
                }
            }
        }
        return matchedIntents
    }

    private fun similarity(a: String, b: String): Double {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val diff = a.zip(b).count { it.first != it.second } + abs(a.length - b.length)
        return 1.0 - diff.toDouble() / maxLen
    }

    private fun getSentenceBasedReply(msg: String): String? {
        return sentenceReplies[msg]
    }


    private fun getProactiveSuggestions(ctx: ChatBotContext, memory: ChatMemory): String {
        val suggestions = mutableListOf<String>()
        if (!ctx.hasWaterData()) suggestions.add("You haven’t logged water today. Try adding a glass now.")
        if (!ctx.hasSleepData()) suggestions.add("No sleep logged. Log bedtime & wake time for insights.")
        if (!ctx.hasMoodData()) suggestions.add("Quick mood check-in can help track emotions.")

        val newSuggestions = suggestions.filter { it != memory.lastSuggestion }
        memory.lastSuggestion = newSuggestions.joinToString(" | ")
        return newSuggestions.joinToString("\n")
    }

    private fun dynamicTone(ctx: ChatBotContext): String {
        return when {
            ctx.mindScore >= 70 -> "🌿 Feeling great today! Keep it up."
            ctx.mindScore in 40..69 -> "🙂 You're doing okay. A few small wins can boost your day."
            else -> "💙 Let’s focus on one step at a time. You’ve got this."
        }
    }

    private fun replyGreeting(ctx: ChatBotContext) = "Hi ${ctx.userName.ifBlank { "there" }} 👋 ${buildQuickInsight(ctx) ?: ""} How can I help you today?"

    private fun buildQuickInsight(ctx: ChatBotContext): String? {
        if (!ctx.hasValidMindScore()) return null
        return when {
            ctx.mindScore >= 70 -> "Your MindScore is looking great today."
            ctx.mindScore >= 40 -> "A few small habits could nudge your score up."
            else -> "Let's focus on one or two small wins today."
        }
    }

    private fun replyMindScore(ctx: ChatBotContext): String {
        if (!ctx.hasValidMindScore()) return "I don’t have enough data yet to show your MindScore. Log water, sleep, mood, or meditation today."

        val statusLine = ctx.mindScoreStatus.takeIf { it.isNotBlank() }?.let { "\n\n$it" } ?: ""
        val suggestion = when {
            ctx.mindScore >= 70 -> " Keep doing what you’re doing."
            ctx.mindScore >= 50 -> " Try adding a small habit: short walk, water, or 2 minutes breathing."
            else -> " Suggestion: log water and sleep if you haven’t, or try a short meditation."
        }
        return "Your MindScore today is ${ctx.mindScore}/100.$statusLine$suggestion"
    }

    private fun replyWater(ctx: ChatBotContext) = if (ctx.hasWaterData()) "Your hydration today: ${ctx.waterSummary}. Staying hydrated supports focus and mood."
    else "You haven’t logged water yet today. Log your first glass to improve your MindScore."

    private fun replySleep(ctx: ChatBotContext) = if (ctx.hasSleepData()) "Your sleep: ${ctx.sleepSummary}. Quality rest helps mood and focus."
    else "No sleep logged today. Log bedtime & wake time for insights."

    private fun replyWorkout(ctx: ChatBotContext) = if (ctx.hasWorkoutData()) "Activity today: ${ctx.workoutSummary}. Movement boosts mental wellness."
    else "No workout logged yet. Even 10 mins of walking helps."

    private fun replyMood(ctx: ChatBotContext) = if (ctx.hasMoodData()) "Your mood today: ${ctx.journalSummary}. Noting how you feel helps track emotions."
    else "You haven’t logged mood today. Quick check-ins improve awareness."

    private fun replyPeriod(ctx: ChatBotContext) = if (ctx.periodSummary.isNotEmpty()) "Period tracking: ${ctx.periodSummary}."
    else "Period tracking isn’t set up yet. Log your cycle for insights."

    private fun replyCalories(ctx: ChatBotContext) = if (ctx.hasCalorieData()) "Calories today: ${ctx.calorieSummary}."
    else "No calorie data yet. Logging meals helps see patterns."

    private fun replyMeditation(ctx: ChatBotContext) = if (ctx.hasMeditationData()) "Mindfulness today: ${ctx.meditationSummary}."
    else "No meditation sessions logged today. Try 2 mins of breathing."

    private fun replyTasks(ctx: ChatBotContext) = if (ctx.hasTaskData()) "Tasks today: ${ctx.taskSummary}."
    else "No tasks logged today. Add 1–2 to track progress."

    private fun replyStress(ctx: ChatBotContext) = "Stress is real 💙. Try 4-4-6 breathing. Meditation helps too."

    private fun replyMotivation(ctx: ChatBotContext): String {
        val quotes = listOf(
            "Small progress is still progress.",
            "Taking care of yourself is productive.",
            "One small step today is worth more than a big plan you never start.",
            "You’re allowed to rest. Rest is part of the process.",
            "Your MindScore is about showing up for yourself."
        )
        return "${quotes[random.nextInt(quotes.size)]} 🌿"
    }

    private fun replyPersonalisedSummary(ctx: ChatBotContext): String {
        val parts = mutableListOf<String>()
        if (ctx.hasValidMindScore()) parts.add("MindScore: ${ctx.mindScore}/100")
        if (ctx.hasWaterData()) parts.add("Water: ${ctx.waterSummary}")
        if (ctx.hasSleepData()) parts.add("Sleep: ${ctx.sleepSummary}")
        if (ctx.hasWorkoutData()) parts.add("Activity: ${ctx.workoutSummary}")
        if (ctx.hasMoodData()) parts.add("Mood: ${ctx.journalSummary}")
        if (ctx.hasTaskData()) parts.add("Tasks: ${ctx.taskSummary}")
        if (ctx.hasMeditationData()) parts.add("Meditation: ${ctx.meditationSummary}")
        if (ctx.hasCalorieData()) parts.add("Calories: ${ctx.calorieSummary}")

        return if (parts.isEmpty()) "You don’t have much logged yet today."
        else "Here’s your day snapshot:\n\n${parts.joinToString("\n") { "• $it" }}"
    }

    private fun replySuggestions(ctx: ChatBotContext): String {
        val suggestions = mutableListOf<String>()
        if (!ctx.hasWaterData()) suggestions.add("Log your first glass of water.")
        if (!ctx.hasSleepData()) suggestions.add("Log last night’s sleep.")
        if (!ctx.hasMoodData()) suggestions.add("Do a quick mood check-in.")
        if (!ctx.hasMeditationData()) suggestions.add("Try a 2-min breathing exercise.")
        if (!ctx.hasWorkoutData()) suggestions.add("Add a short walk or stretch.")
        if (!ctx.hasTaskData()) suggestions.add("Add 1–2 tasks and complete one.")

        return if (suggestions.isEmpty()) "You’re already doing a lot today. Keep going 🌿"
        else "Suggestions:\n${suggestions.take(4).mapIndexed { i,s -> "${i+1}. $s" }.joinToString("\n")}"
    }

    private fun replyHelp() = """
        I'm your MindNest wellness assistant. Ask me about:
        • MindScore, Water, Sleep, Workout
        • Mood / Journal, Period, Calories, Meditation
        • Tasks, Summary / Insights, Suggestions
        • Stress / Anxiety, Motivation
    """.trimIndent()

    private fun replyFallback(ctx: ChatBotContext, memory: ChatMemory) = buildQuickInsight(ctx)?.let {
        "$it You can ask me for summary, suggestions, or about MindScore, sleep, water, mood, or stress."
    } ?: "I'm not sure I understood. Try asking for summary, suggestions, or MindScore, sleep, water, mood, or stress."
}
