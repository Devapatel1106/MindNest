package com.example.mindnest

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        DailyTaskFragment(),
        MeditationFragment(),
        JournalFragment(),
        SleepTrackingFragment(),
        WaterIntakeFragment(),
        WorkoutFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}
