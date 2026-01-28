package com.example.mindnest

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mindnest.databinding.ActivityViewPagerBinding
import com.example.mindnest.utils.PreferenceManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ViewPager : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityViewPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSkip.paintFlags =
            binding.btnSkip.paintFlags or Paint.UNDERLINE_TEXT_FLAG


        val gender = PreferenceManager(this).getUserGender() ?: "male"
        binding.viewPager.adapter = MyViewPagerAdapter(this, gender)


        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, _ ->
            tab.customView = layoutInflater.inflate(R.layout.tab_dot, null)
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.isSelected = true
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.isSelected = false
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.btnSkip.setOnClickListener(this)
        binding.btnNext.setOnClickListener(this)
        binding.btnPrevious.setOnClickListener(this)

        binding.btnFinish.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            intent.getStringExtra("USER_NAME")?.let { dashboardIntent.putExtra("USER_NAME", it) }
            intent.getStringExtra("USER_EMAIL")?.let { dashboardIntent.putExtra("USER_EMAIL", it) }
            startActivity(dashboardIntent)
            finish()
        }

        updateButtons(0)

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateButtons(position)
                }
            }
        )

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startActivity(
                        Intent(this@ViewPager, LogInActivity::class.java)
                    )
                    finish()
                }
            })
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnSkip -> {
                val dashboardIntent = Intent(this, DashboardActivity::class.java)
                intent.getStringExtra("USER_NAME")?.let { dashboardIntent.putExtra("USER_NAME", it) }
                intent.getStringExtra("USER_EMAIL")?.let { dashboardIntent.putExtra("USER_EMAIL", it) }
                startActivity(dashboardIntent)
                finish()
            }

            R.id.btnNext -> {
                val current = binding.viewPager.currentItem
                val last = binding.viewPager.adapter!!.itemCount - 1
                if (current < last) {
                    binding.viewPager.currentItem = current + 1
                } else {
                    val dashboardIntent = Intent(this, DashboardActivity::class.java)
                    intent.getStringExtra("USER_NAME")?.let { dashboardIntent.putExtra("USER_NAME", it) }
                    intent.getStringExtra("USER_EMAIL")?.let { dashboardIntent.putExtra("USER_EMAIL", it) }
                    startActivity(dashboardIntent)
                    finish()
                }
            }

            R.id.btnPrevious -> {
                val prev = binding.viewPager.currentItem - 1
                if (prev >= 0) binding.viewPager.currentItem = prev
            }
        }
    }

    private fun updateButtons(position: Int) {
        val last = binding.viewPager.adapter!!.itemCount - 1

        binding.btnPrevious.visibility =
            if (position == 0) View.INVISIBLE else View.VISIBLE

        if (position == last) {
            binding.btnNext.visibility = View.GONE
            binding.btnFinish.visibility = View.VISIBLE
        } else {
            binding.btnNext.visibility = View.VISIBLE
            binding.btnFinish.visibility = View.GONE
        }
    }
}
