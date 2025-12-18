package com.example.mindnest

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mindnest.databinding.ActivityViewPagerBinding

class ViewPager : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityViewPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSkip.paintFlags =
            binding.btnSkip.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        binding.viewPager.adapter = MyViewPagerAdapter(this)

        binding.btnSkip.setOnClickListener(this)
        binding.btnNext.setOnClickListener(this)
        binding.btnPrevious.setOnClickListener(this)

        updateButtons(0)

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateButtons(position)
                }
            }
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@ViewPager, LogInActivity::class.java))
                finish()
            }
        })
    }

    override fun onClick(view: View) {
        when (view.id) {

            R.id.btnSkip -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }

            R.id.btnNext -> {
                val current = binding.viewPager.currentItem
                val lastPage = binding.viewPager.adapter!!.itemCount - 1

                if (current == lastPage) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    binding.viewPager.currentItem = current + 1
                }
            }

            R.id.btnPrevious -> {
                val prevItem = binding.viewPager.currentItem - 1
                if (prevItem >= 0) {
                    binding.viewPager.currentItem = prevItem
                }
            }
        }
    }

    private fun updateButtons(position: Int) {
        binding.btnPrevious.visibility =
            if (position == 0) View.INVISIBLE else View.VISIBLE
    }
}
