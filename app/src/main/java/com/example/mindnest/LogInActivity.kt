package com.example.mindnest

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mindnest.databinding.ActivityLoginBinding
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.launch

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { application as MindNestApplication }
    private val preferenceManager by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener { handleLogin() }
        setSignUpLink()
        handleKeyboardScroll()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        resetErrors()
    }

    private fun setSignUpLink() {
        val text = "Don't have an account? Sign up"
        val spannable = SpannableString(text)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LogInActivity, CreateAccountActivity::class.java))
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(this@LogInActivity, R.color.lavender_primary)
            }
        }

        spannable.setSpan(
            clickableSpan,
            text.indexOf("Sign up"),
            text.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.createAccountTxt.text = spannable
        binding.createAccountTxt.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun handleKeyboardScroll() {
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

    private fun showError(
        editText: AppCompatEditText,
        errorTextView: TextView,
        message: String
    ) {
        editText.background =
            ContextCompat.getDrawable(this, R.drawable.edit_text_error)

        val drawable = editText.compoundDrawablesRelative[0]?.mutate()
        drawable?.setTint(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
        editText.requestFocus()
    }

    private fun clearError(editText: AppCompatEditText, errorTextView: TextView) {
        editText.background = ContextCompat.getDrawable(this, R.drawable.edit_text)
        val drawable = editText.compoundDrawablesRelative[0]?.mutate()
        drawable?.setTint(ContextCompat.getColor(this, R.color.lavender_primary))
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        errorTextView.visibility = View.GONE
    }

    private fun resetErrors() {
        clearError(binding.email, binding.emailErrorTxt)
        clearError(binding.edtPassword, binding.passwordErrorTxt)
    }

    private fun handleLogin() {
        val email = binding.email.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()

        resetErrors()

        when {
            email.isEmpty() ->
                showError(binding.email, binding.emailErrorTxt, "Please enter your Email Address")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                showError(binding.email, binding.emailErrorTxt, "Enter a valid email")
            password.isEmpty() ->
                showError(binding.edtPassword, binding.passwordErrorTxt, "Password is required")
            else -> {
                lifecycleScope.launch {
                    try {
                        val user = app.userRepository.login(email, password)
                        if (user != null) {

                            preferenceManager.saveUserId(user.id)
                            preferenceManager.saveUserName(user.name)
                            preferenceManager.saveUserEmail(user.email)

                            val intent = Intent(this@LogInActivity, DashboardActivity::class.java)
                            intent.putExtra("USER_NAME", user.name)
                            intent.putExtra("USER_EMAIL", user.email)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LogInActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LogInActivity, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
