package com.example.mindnest.data

import com.example.mindnest.model.Quote

object QuoteDataSource {

    val quoteList = listOf(


        Quote("Peace comes from within. Do not seek it without.", "Buddha", "Meditation"),
        Quote("The present moment is filled with joy and happiness. If you are attentive, you will see it.", category = "Meditation"),
        Quote("Meditation is not evading life; it’s entering it fully.", category = "Meditation"),
        Quote("Breathe. Let go. And remind yourself that this moment is the only one you know you have for sure.", category = "Meditation"),


        Quote("Your body can stand almost anything. It’s your mind that you have to convince.", category = "Workout"),
        Quote("Don’t limit your challenges. Challenge your limits.", category = "Workout"),
        Quote("Sweat is fat crying.", category = "Workout"),
        Quote("Strength does not come from physical capacity. It comes from an indomitable will.", "Mahatma Gandhi", "Workout"),


        Quote("Drinking water is like washing out your insides.", category = "Water"),
        Quote("There is no life without water.", "Albert Szent-Gyorgyi", "Water"),


        Quote("Sleep is the best meditation.", "Dalai Lama", "Sleep"),
        Quote("Your future depends on your dreams, so go to sleep.", category = "Sleep"),
        Quote("Rest and self-care allow you to give your best to the world.", category = "Sleep"),


        Quote("The more you write, the more you realize who you are.", category = "Journal"),
        Quote("Journaling is like whispering to yourself and listening at the same time.", "Mina Murray", "Journal"),
        Quote("Your journal is your mirror; reflect, don’t deflect.", category = "Journal"),


        Quote("Your cycle is a superpower, not a burden.", category = "Period"),
        Quote("A strong woman knows when to rise, rest, and reset.", category = "Period"),
        Quote("Listen to your body. It talks in cycles and rhythms.", category = "Period"),


        Quote("Don’t dig your grave with your own knife and fork.", "English Proverb", "Nutrition"),
        Quote("Eat to fuel your body, not your emotions.", category = "Nutrition"),
        Quote("Take care of your body. It’s the only place you have to live.", "Jim Rohn", "Nutrition"),
        Quote("Every meal is a chance to nourish your future self.", category = "Nutrition"),


        Quote("Small steps every day lead to big results.", category = "General"),
        Quote("Progress, not perfection.", category = "General"),
        Quote("Your only limit is your mind.", category = "General"),
        Quote("Consistency is the key to success.", category = "General"),
        Quote("The best project you’ll ever work on is you.", category = "General")
    )


    fun getAllQuotes(): List<Quote> = quoteList


    fun getQuotesByCategory(category: String): List<Quote> {
        return quoteList.filter { it.category == category }
    }
    fun getRandomQuote(): String {
        return quoteList.random().text
    }

}
