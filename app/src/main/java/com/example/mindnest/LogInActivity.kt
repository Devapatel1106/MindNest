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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.mindnest.data.entity.User

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { application as MindNestApplication }
    private val preferenceManager by lazy { PreferenceManager(this) }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (preferenceManager.getUserId() != -1L) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener { handleLogin() }
        setSignUpLink()
        handleKeyboardScroll()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishAffinity() }
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
        drawable?.setTint(
            ContextCompat.getColor(
                this,
                android.R.color.holo_red_dark
            )
        )

        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            drawable,
            null,
            null,
            null
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

        val drawable = editText.compoundDrawablesRelative[0]?.mutate()
        drawable?.setTint(
            ContextCompat.getColor(
                this,
                R.color.lavender_primary
            )
        )

        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            drawable,
            null,
            null,
            null
        )

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

                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->

                        val firebaseUser = authResult.user ?: return@addOnSuccessListener
                        val uid = firebaseUser.uid
                        val userEmail = firebaseUser.email ?: email

                        lifecycleScope.launch {

                            var user = app.userRepository.getUserByEmail(userEmail)

                            if (user == null) {

                                val doc = firestore.collection("users")
                                    .document(uid)
                                    .get()
                                    .await()

                                if (doc.exists()) {

                                    val name = doc.getString("name") ?: "User"
                                    val gender = doc.getString("gender") ?: ""

                                    val newUser = User(
                                        uid = uid,
                                        name = name,
                                        email = userEmail,
                                        password = "",
                                        gender = gender
                                    )

                                    val id = app.userRepository.register(newUser)
                                    user = newUser.copy(id = id)
                                }
                            }

                            if (user == null) {
                                Toast.makeText(
                                    this@LogInActivity,
                                    "User data not found",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }

                            preferenceManager.saveUserId(user.id)
                            preferenceManager.saveUserName(user.name)
                            preferenceManager.saveUserEmail(user.email)
                            preferenceManager.saveUserGender(user.gender)

                            startActivity(
                                Intent(this@LogInActivity, DashboardActivity::class.java)
                            )
                            finish()

                            try {
                                app.taskRepository.startRealtimeSync(user.id)
                                app.waterRepository.startRealtimeSync(user.id)
                                app.sleepRepository.startRealtimeSync(user.id)
                                app.workoutRepository.startRealtimeSync(user.id)
                                app.periodRepository.syncPeriodFromFirebase(user.id)
                                app.calorieRepository.startUserRealtimeSync(user.id.toString())
                                app.calorieRepository.startFoodRealtimeSync(user.id.toString())
                                app.journalRepository.startRealtimeSync(user.id)
                                app.mindScoreRepository.startRealtimeSync(user.id)
                                app.chatRepository.syncChatFromFirebase(user.id)
                                app.userSettingsRepository.startRealtimeSync(user.id)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Login failed: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }
}