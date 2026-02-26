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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding
    private val app by lazy { application as MindNestApplication }
    private val preferenceManager by lazy { PreferenceManager(this) }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.SignInBtn.setOnClickListener { handleSignUp() }

        setLoginRedirectLink()
        handleKeyboardScroll()

        // TextWatcher for gender icon update
        binding.edtGender.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim().lowercase()
                val iconRes = when (text) {
                    "male", "m", "boy" -> R.drawable.male_24px
                    "female", "f", "girl" -> R.drawable.female_24px
                    else -> 0
                }

                binding.edtGender.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(this@CreateAccountActivity, R.drawable.person_24px),
                    null,
                    if (iconRes != 0)
                        ContextCompat.getDrawable(this@CreateAccountActivity, iconRes)
                    else null,
                    null
                )
            }
        })
    }

    private fun handleSignUp() {
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        val genderInput = binding.edtGender.text.toString().trim().lowercase()

        val selectedGender = when (genderInput) {
            "male", "m", "boy" -> "Male"
            "female", "f", "girl" -> "Female"
            else -> ""
        }

        when {
            name.isEmpty() -> {
                showError(binding.name, binding.nameErrorTxt, "Name required")
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(binding.email, binding.emailErrorTxt, "Valid email required")
                return
            }
            password.isEmpty() -> {
                showError(binding.edtPassword, binding.passwordErrorTxt, "Password required")
                return
            }
            selectedGender.isEmpty() -> {
                showError(binding.edtGender, binding.genderErrorTxt, "Enter Male or Female")
                return
            }
        }

        binding.SignInBtn.isEnabled = false

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val firebaseUser = authResult.user
                val uid = firebaseUser?.uid ?: ""

                if (uid.isEmpty()) {
                    Toast.makeText(this, "UID error", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userMap = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "gender" to selectedGender,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(uid)
                    .set(userMap)
                    .addOnSuccessListener {

                        lifecycleScope.launch {
                            try {
                                val user = User(
                                    uid = uid,
                                    name = name,
                                    email = email,
                                    password = password,
                                    gender = selectedGender
                                )

                                val userId = app.userRepository.register(user)

                                // Save all user info locally
                                preferenceManager.saveUserId(userId)
                                preferenceManager.saveUserName(name)
                                preferenceManager.saveUserEmail(email)
                                preferenceManager.saveUserGender(selectedGender) // Important!

                                Toast.makeText(
                                    this@CreateAccountActivity,
                                    "Account Created",
                                    Toast.LENGTH_SHORT
                                ).show()

                                startActivity(
                                    Intent(this@CreateAccountActivity, ViewPager::class.java)
                                )
                                finish()

                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@CreateAccountActivity,
                                    "Local DB Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        binding.SignInBtn.isEnabled = true
                        Toast.makeText(this, "Firestore Error", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.SignInBtn.isEnabled = true
                Toast.makeText(this, "Signup Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showError(editText: AppCompatEditText, errorTextView: TextView, message: String) {
        editText.background = ContextCompat.getDrawable(this, R.drawable.edit_text_error)
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
        editText.requestFocus()
    }

    private fun setLoginRedirectLink() {
        binding.loginRedirectTxt.setOnClickListener {
            startActivity(Intent(this, LogInActivity::class.java))
            finish()
        }
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
}