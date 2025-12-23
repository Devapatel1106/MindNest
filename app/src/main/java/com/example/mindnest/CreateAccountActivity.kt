package com.example.mindnest

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.example.mindnest.databinding.ActivityCreateAccountBinding

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.SignInBtn.setOnClickListener {
            handleSignUp()
        }

        setLoginRedirectLink()
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                binding.rootScroll.scrollTo(0, binding.email.bottom)
            }
        }
    }

    private fun setLoginRedirectLink() {
        val fullText = "Already have an account? Log in"
        val spannable = android.text.SpannableString(fullText)

        val start = fullText.indexOf("Log in")
        val end = start + "Log in".length

        val clickableSpan = object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@CreateAccountActivity, LogInActivity::class.java))
                finish()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = resources.getColor(R.color.lavender_primary)
            }
        }

        spannable.setSpan(
            clickableSpan,
            start,
            end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.loginRedirectTxt.text = spannable
        binding.loginRedirectTxt.movementMethod =
            android.text.method.LinkMovementMethod.getInstance()
    }

    private fun showError(
        editText: AppCompatEditText,
        errorTextView: TextView,
        message: String
    ) {
        editText.background =
            ContextCompat.getDrawable(this, R.drawable.edit_text_error)

        editText.compoundDrawablesRelative[0]?.setTint(
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )

        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
        editText.requestFocus()
    }

    private fun clearError(
        editText: AppCompatEditText,
        errorTextView: TextView
    ) {

        editText.background =
            ContextCompat.getDrawable(this, R.drawable.edit_text)


        editText.compoundDrawablesRelative[0]?.setTint(
            ContextCompat.getColor(this, R.color.lavender_primary)
        )

        errorTextView.visibility = View.GONE
    }

    private fun handleSignUp() {
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()

        clearError(binding.name, binding.nameErrorTxt)
        clearError(binding.email, binding.emailErrorTxt)
        clearError(binding.edtPassword, binding.passwordErrorTxt)

        when {
            name.isEmpty() -> {
                showError(binding.name, binding.nameErrorTxt, "Name is required")
            }

            email.isEmpty() -> {
                showError(
                    binding.email,
                    binding.emailErrorTxt,
                    "Please enter your Email Address"
                )
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(binding.email, binding.emailErrorTxt, "Enter a valid email")
            }

            password.isEmpty() -> {
                showError(
                    binding.edtPassword,
                    binding.passwordErrorTxt,
                    "Password is required"
                )
            }

            else -> {
                Toast.makeText(
                    this,
                    "Account created successfully",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this, com.example.mindnest.ViewPager::class.java))
                finish()
            }
        }
    }
}
