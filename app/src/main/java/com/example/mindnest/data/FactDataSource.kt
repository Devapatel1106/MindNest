package com.example.mindnest.data

import com.example.mindnest.model.Fact

object FactDataSource {

    val factList = listOf(


        Fact("Meditation can reduce stress reactivity in as little as 8 weeks.", "Meditation"),
        Fact("Mindful breathing improves heart-rate variability and resilience.", "Meditation"),
        Fact("Experienced meditators show higher learning-related brain activity.", "Meditation"),
        Fact("Mindfulness strengthens decision-making brain regions.", "Meditation"),


        Fact("Muscles remember previous training and regain strength faster.", "Workout"),
        Fact("Resistance training improves bone density.", "Workout"),
        Fact("HIIT releases proteins that support brain growth.", "Workout"),
        Fact("Exercise boosts both physical and mental health.", "Workout"),


        Fact("Mild dehydration can reduce cognitive performance.", "Water"),
        Fact("Morning water helps activate metabolism.", "Water"),
        Fact("Cold water slightly increases calorie burn.", "Water"),


        Fact("Deep sleep helps the brain remove toxins.", "Sleep"),
        Fact("REM sleep improves emotional memory.", "Sleep"),
        Fact("Morning sunlight improves sleep quality.", "Sleep"),
        Fact("Good sleep boosts immunity and focus.", "Sleep"),


        Fact("Writing by hand improves memory and emotional processing.", "Journal"),
        Fact("Gratitude journaling increases dopamine levels.", "Journal"),
        Fact("Daily reflection reveals hidden behavior patterns.", "Journal"),


        Fact("Exercise helps reduce period mood swings.", "Period"),
        Fact("Hormone changes affect sleep quality.", "Period"),
        Fact("Tracking cycles helps predict ovulation.", "Period"),


        Fact("Eating slowly helps prevent overeating.", "Nutrition"),
        Fact("Protein before sleep supports muscle repair.", "Nutrition"),
        Fact("Gut bacteria influence metabolism and appetite.", "Nutrition"),
        Fact("Dark chocolate can improve heart health.", "Nutrition"),


        Fact("Smiling can lower stress hormones.", "General"),
        Fact("Deep breathing activates relaxation response.", "General"),
        Fact("Writing goals increases success by 42%.", "General"),
        Fact("Nature exposure lowers cortisol.", "General"),
        Fact("Green spaces improve mood and focus.", "General")
    )

    fun getAllFacts(): List<Fact> = factList

    fun getFactsByCategory(category: String): List<Fact> {
        return factList.filter { it.category == category }
    }
}
