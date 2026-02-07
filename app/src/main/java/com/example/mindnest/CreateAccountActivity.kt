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
import androidx.lifecycle.lifecycleScope
import com.example.mindnest.data.entity.User
import com.example.mindnest.databinding.ActivityCreateAccountBinding
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding
    private val app by lazy { application as MindNestApplication }
    private val preferenceManager by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.SignInBtn.setOnClickListener { handleSignUp() }
        setLoginRedirectLink()
        handleKeyboardScroll()

        binding.edtGender.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim().lowercase()
                val iconRes = when (text) {
                    "male", "boy" -> R.drawable.male_24px
                    "female", "girl" -> R.drawable.female_24px
                    else -> 0
                }
                binding.edtGender.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(this@CreateAccountActivity, R.drawable.person_24px),
                    null,
                    if (iconRes != 0) ContextCompat.getDrawable(this@CreateAccountActivity, iconRes) else null,
                    null
                )
            }
        })
    }

    override fun onResume() { super.onResume(); resetErrors() }

    private fun setLoginRedirectLink() {
        val text = "Already have an account? Log in"
        val spannable = android.text.SpannableString(text)
        val clickableSpan = object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@CreateAccountActivity, LogInActivity::class.java))
                finish()
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(this@CreateAccountActivity, R.color.lavender_primary)
            }
        }
        spannable.setSpan(clickableSpan, text.indexOf("Log in"), text.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.loginRedirectTxt.text = spannable
        binding.loginRedirectTxt.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun handleKeyboardScroll() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) binding.rootScroll.scrollTo(0, binding.email.bottom)
        }
    }

    private fun showError(editText: AppCompatEditText, errorTextView: TextView, message: String) {
        editText.background = ContextCompat.getDrawable(this, R.drawable.edit_text_error)
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
        clearError(binding.name, binding.nameErrorTxt)
        clearError(binding.email, binding.emailErrorTxt)
        clearError(binding.edtPassword, binding.passwordErrorTxt)
        clearError(binding.edtGender, binding.genderErrorTxt)
    }

    private fun handleSignUp() {
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        val genderInput = binding.edtGender.text.toString().trim().lowercase()
        resetErrors()

        val selectedGender = when (genderInput) {
            "male", "boy" -> "male"
            "female", "girl" -> "female"
            else -> ""
        }

        when {
            name.isEmpty() -> showError(binding.name, binding.nameErrorTxt, "Name is required")
            email.isEmpty() -> showError(binding.email, binding.emailErrorTxt, "Please enter your Email Address")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError(binding.email, binding.emailErrorTxt, "Enter a valid email")
            password.isEmpty() -> showError(binding.edtPassword, binding.passwordErrorTxt, "Password is required")
            selectedGender.isEmpty() -> showError(binding.edtGender, binding.genderErrorTxt, "Please enter a valid gender (Male/Female)")
            else -> {
                lifecycleScope.launch {
                    try {
                        val existingUser = app.userRepository.getUserByEmail(email)
                        if (existingUser != null) {
                            Toast.makeText(this@CreateAccountActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val user = User(name = name, email = email, password = password, gender = selectedGender)
                        val userId = app.userRepository.register(user)

                        preferenceManager.saveUserId(userId)
                        preferenceManager.saveUserName(name)
                        preferenceManager.saveUserEmail(email)
                        preferenceManager.saveUserGender(selectedGender)

                        Toast.makeText(this@CreateAccountActivity, "Account created successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@CreateAccountActivity, ViewPager::class.java)
                        intent.putExtra("USER_NAME", name)
                        intent.putExtra("USER_EMAIL", email)
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@CreateAccountActivity, "Error creating account: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
