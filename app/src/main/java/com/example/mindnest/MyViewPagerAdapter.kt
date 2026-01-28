package com.example.mindnest

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyViewPagerAdapter(
    activity: FragmentActivity,
    private val gender: String
) : FragmentStateAdapter(activity) {

    private val fragments: List<Fragment>
        get() = if (gender.lowercase() == "female") {
            listOf(
                DailyTaskFragment(),
                MeditationFragment(),
                JournalFragment(),
                SleepTrackingFragment(),
                WaterIntakeFragment(),
                WorkoutFragment(),
                PeriodFragment(),
                CalorieCountFragment()
            )
        } else {
            listOf(
                DailyTaskFragment(),
                MeditationFragment(),
                JournalFragment(),
                SleepTrackingFragment(),
                WaterIntakeFragment(),
                WorkoutFragment(),
                CalorieCountFragment()
            )
        }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
