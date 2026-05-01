package com.example.mindnest.data

import android.content.Context
import java.util.*
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
    val pastWeekSleep: List<Double> = emptyList(),
    val pastWeekMindScore: List<Int> = emptyList()
) {
    fun hasSleep() = sleepSummary.isNotBlank() && !sleepSummary.contains("No", true)
    fun hasWater() = waterSummary.isNotBlank() && !waterSummary.contains("No", true)
    fun hasWorkout() = workoutSummary.isNotBlank() && !workoutSummary.contains("No", true)
    fun hasTasks() = taskSummary.isNotBlank() && !taskSummary.contains("No", true)
    fun hasMood() = journalSummary.isNotBlank()
    fun validScore() = mindScore in 1..100
}

data class ChatMemory(
    var lastTopics: List<String> = emptyList(),
    var emotion: String = "neutral",
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

        val msg = message.lowercase().trim()
        if (msg.isBlank()) return "I'm here. Tell me what's on your mind."

        val emotion = detectEmotion(msg)
        val intents = detectIntents(msg)

        memory.emotion = emotion
        memory.lastTopics = intents

        val reply = when {
            isFact(msg) -> smartFact(ctx)
            msg.contains("why") -> whyAnalysis(ctx)
            msg.contains("plan") || msg.contains("improve") -> actionPlan(ctx)
            msg.contains("summary") -> summary(ctx)
            else -> buildResponse(msg, intents, emotion, ctx, memory)
        }

        memory.history.add(message)
        memory.history.add(reply)

        return reply
    }
    fun getSuggestions(ctx: ChatBotContext): List<String> {

        val list = mutableListOf(
            "How am I doing today?",
            "Motivate me",
            "Improve my sleep",
            "Reduce stress",
            "Analyze my habits"
        )

        if (!ctx.hasWorkout()) list.add("How should I start working out?")
        if (!ctx.hasSleep()) list.add("Fix my sleep schedule")
        if (!ctx.hasWater()) list.add("Increase hydration")
        if (ctx.validScore()) list.add("Explain my MindScore")

        return list.shuffled().take(6)
    }
    fun dailyBrief(ctx: ChatBotContext): String {

        val insights = mutableListOf<String>()

        if (!ctx.hasSleep()) insights.add("Sleep is not optimized")
        if (!ctx.hasWater()) insights.add("Hydration is low")
        if (!ctx.hasWorkout()) insights.add("No physical activity detected")

        val focus = if (insights.isEmpty())
            "Maintain consistency across all habits"
        else
            insights.joinToString(" + ")

        return """
Good day ${ctx.userName} ☀️

Here’s your daily intelligence briefing:

• MindScore: ${ctx.mindScore}/100
• Key gaps: $focus

Today’s focus:
→ Improve weakest habit
→ Complete 1 high-value task early

Small improvements today will compound.
        """.trimIndent()
    }

    private fun detectEmotion(msg: String): String {
        return when {
            listOf("sad","low","tired").any { msg.contains(it) } -> "low"
            listOf("stress","anxious","overwhelmed").any { msg.contains(it) } -> "stress"
            listOf("good","great","happy").any { msg.contains(it) } -> "positive"
            else -> "neutral"
        }
    }

    private fun detectIntents(msg: String): List<String> {

        val intents = mutableListOf<String>()

        if (msg.contains("sleep")) intents.add("sleep")
        if (msg.contains("workout") || msg.contains("gym")) intents.add("workout")
        if (msg.contains("water")) intents.add("water")
        if (msg.contains("task")) intents.add("task")
        if (msg.contains("stress")) intents.add("stress")
        if (msg.contains("food")) intents.add("food")

        return if (intents.isEmpty()) listOf("general") else intents
    }

    private fun buildResponse(
        msg: String,
        intents: List<String>,
        emotion: String,
        ctx: ChatBotContext,
        memory: ChatMemory
    ): String {

        val tone = when (emotion) {
            "low" -> "I understand you're not feeling your best."
            "stress" -> "It seems like you're under pressure."
            "positive" -> "That's great to hear."
            else -> "Got it."
        }

        val insights = intents.mapNotNull { insight(it, ctx) }
        val actions = intents.mapNotNull { action(it) }

        return listOf(
            tone,
            insights.joinToString("\n"),
            actions.joinToString("\n"),
            efficiency(ctx),
            followUp(intents),
            memoryLine(memory)
        ).filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun insight(intent: String, ctx: ChatBotContext): String? {

        return when (intent) {

            "sleep" ->
                if (!ctx.hasSleep())
                    "Your sleep may be affecting energy and focus."
                else
                    "Your sleep is influencing mood and recovery."

            "workout" ->
                if (!ctx.hasWorkout())
                    "Lack of movement can reduce mental clarity."
                else
                    "Your workouts are supporting your mental state."

            "water" ->
                if (!ctx.hasWater())
                    "Low hydration may be affecting your energy."
                else
                    "Your hydration supports cognitive performance."

            else -> null
        }
    }

    private fun action(intent: String): String? {

        return when (intent) {
            "sleep" -> "Fix your sleep schedule and reduce screen exposure."
            "workout" -> "Add 20–30 minutes of daily movement."
            "water" -> "Drink 2–3L water daily."
            "task" -> "Start with one high-impact task."
            else -> null
        }
    }

    private fun efficiency(ctx: ChatBotContext): String {

        val list = mutableListOf<String>()

        if (!ctx.hasSleep()) list.add("Fix sleep")
        if (!ctx.hasWorkout()) list.add("Add movement")
        if (!ctx.hasWater()) list.add("Increase hydration")

        return if (list.isEmpty()) {
            "You're well balanced. Focus on consistency."
        } else {
            "Focus now:\n• ${list.joinToString("\n• ")}"
        }
    }

    private fun followUp(intents: List<String>): String {
        return when {
            "sleep" in intents -> "Has your sleep been inconsistent lately?"
            "stress" in intents -> "What’s causing the most stress right now?"
            else -> "What would you like to improve next?"
        }
    }

    private fun memoryLine(memory: ChatMemory): String {
        return if (memory.history.size > 4)
            "You’ve mentioned similar concerns earlier."
        else ""
    }

    private fun isFact(msg: String): Boolean {
        return listOf("fact","interesting","tell me something").any { msg.contains(it) }
    }

    private fun smartFact(ctx: ChatBotContext): String {

        val facts = mutableListOf(
            "Poor sleep can reduce focus by up to 30%.",
            "Even slight dehydration impacts mood and energy.",
            "Exercise improves brain function instantly.",
            "Your brain uses 20% of your body’s energy."
        )

        if (!ctx.hasSleep()) facts.add("Improving sleep will significantly boost your performance.")
        if (!ctx.hasWorkout()) facts.add("Adding movement will improve your mental clarity.")

        return facts.random()
    }

    private fun whyAnalysis(ctx: ChatBotContext): String {

        return """
Here’s what might be happening:

• Sleep: ${ctx.sleepSummary}
• Workout: ${ctx.workoutSummary}
• Water: ${ctx.waterSummary}

These factors together directly influence your mood and performance.

This isn’t random — it’s pattern-based.
        """.trimIndent()
    }

    private fun actionPlan(ctx: ChatBotContext): String {

        return """
Let’s improve step by step:

Step 1: Fix sleep timing
Step 2: Add 20 min movement daily
Step 3: Complete 1 important task early

Start simple — build consistency first.
        """.trimIndent()
    }

    private fun summary(ctx: ChatBotContext): String {

        return """
Current status:

MindScore: ${ctx.mindScore}/100
Sleep: ${ctx.sleepSummary}
Workout: ${ctx.workoutSummary}
Water: ${ctx.waterSummary}
Mood: ${ctx.journalSummary}

You're building your system gradually.
        """.trimIndent()
    }

}