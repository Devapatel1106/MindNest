package com.example.mindnest

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.mindnest.databinding.ActivityLoginBinding
import androidx.activity.OnBackPressedCallback

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener {
            handleLogin()
        }

        setSignUpLink()
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                binding.rootScroll.scrollTo(0, binding.email.bottom)
            }
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    private fun setSignUpLink() {
        val fullText = "Don't have an account? Sign up"
        val spannable = SpannableString(fullText)

        val start = fullText.indexOf("Sign up")
        val end = start + "Sign up".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(
                    Intent(this@LogInActivity, CreateAccountActivity::class.java)
                )
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = resources.getColor(R.color.lavender_primary)
            }
        }

        spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.createAccountTxt.text = spannable
        binding.createAccountTxt.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showError(
        editText: AppCompatEditText,
        errorTextView: TextView,
        message: String
    ) {

        editText.background =
            ContextCompat.getDrawable(this, R.drawable.edit_text_error)

        editText.compoundDrawablesRelative[0]?.setTint(ContextCompat.getColor(this, android.R.color.holo_red_dark))

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

        editText.compoundDrawablesRelative[0]?.setTint(ContextCompat.getColor(this, R.color.lavender_primary))

        errorTextView.visibility = View.GONE
    }

    private fun handleLogin() {
        val email = binding.email.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()

        clearError(binding.email, binding.emailErrorTxt)
        clearError(binding.edtPassword, binding.passwordErrorTxt)

        when {
            email.isEmpty() -> {
                showError(
                    binding.email,
                    binding.emailErrorTxt,
                    "Please enter your Email Address"
                )
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(
                    binding.email,
                    binding.emailErrorTxt,
                    "Enter a valid email"
                )
            }

            password.isEmpty() -> {
                showError(
                    binding.edtPassword,
                    binding.passwordErrorTxt,
                    "Password is required"
                )
            }

            else -> {
                startActivity(Intent(this, ViewPager::class.java))
                finish()
            }
        }
    }

}
