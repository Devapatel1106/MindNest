package com.example.mindnest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnest.databinding.ActivityGetStartedBinding

class GetStartedActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityGetStartedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        runIntroAnimation()

        binding.startBtn.setOnClickListener(this)
    }

    private fun runIntroAnimation() {

        binding.logoImage.alpha = 0f
        binding.logoImage.translationY = 40f

        binding.titleText.alpha = 0f
        binding.subtitleText.alpha = 0f

        binding.homeImage.alpha = 0f
        binding.homeImage.translationY = 30f
        binding.homeImage.scaleX = 0.95f
        binding.homeImage.scaleY = 0.95f

        binding.bottomLayout.alpha = 0f
        binding.bottomLayout.translationY = 60f

        binding.logoImage.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.titleText.animate()
            .alpha(1f)
            .setStartDelay(250)
            .setDuration(500)
            .start()

        binding.subtitleText.animate()
            .alpha(1f)
            .setStartDelay(400)
            .setDuration(500)
            .start()

        binding.homeImage.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(550)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.bottomLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(750)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.startBtn -> {
                animateButtonClick {
                    val intent = Intent(this, LogInActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
    }

    private fun animateButtonClick(onEnd: () -> Unit) {
        binding.startBtn.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                binding.startBtn.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .withEndAction { onEnd() }
                    .start()
            }
            .start()
    }
}
