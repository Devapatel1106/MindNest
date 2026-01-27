package com.example.mindnest


import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.mindnest.ui.CalorieFragment
import com.example.mindnest.databinding.ActivityDashboardBinding
import com.example.mindnest.ui.journal.JournalMoodFragment
import com.example.mindnest.ui.mindfulness.FragmentMindfulness
import com.example.mindnest.ui.periodtracker.PeriodTrackerFragment
import com.example.mindnest.ui.water.WaterFragment
import com.example.mindnest.ui.workout.WorkoutTrackingFragment
import com.example.mindnest.utils.PreferenceManager


class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private val preferenceManager by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = getColor(R.color.white)

        setNavHeaderData()
        setupNavigationMenu()
        setupLogout()

        if (savedInstanceState == null) {
            binding.navigationView.setCheckedItem(R.id.nav_overview)
            binding.toolbar.title = "Overview"
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    AlertDialog.Builder(this@DashboardActivity)
                        .setTitle("Exit App")
                        .setMessage("Are you sure you want to exit?")
                        .setPositiveButton("OK") { _, _ ->
                            finishAffinity()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        })
    }

    private fun setNavHeaderData() {
        val headerView = binding.navigationView.getHeaderView(0)

        val txtUserName = headerView.findViewById<TextView>(R.id.txtUserName)
        val txtUserEmail = headerView.findViewById<TextView>(R.id.txtUserEmail)


        val name = intent.getStringExtra("USER_NAME") ?: preferenceManager.getUserName()
        val email = intent.getStringExtra("USER_EMAIL") ?: preferenceManager.getUserEmail()

        txtUserName.text = name ?: "User"
        txtUserEmail.text = email ?: "user@email.com"
    }

    private fun setupNavigationMenu() {
        binding.navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {
                R.id.nav_tasks -> {
                    loadFragment(FragmentTask())
                    binding.toolbar.title = item.title
                }
                R.id.nav_meditation -> {
                    loadFragment(FragmentMindfulness())
                    binding.toolbar.title = item.title
                }
                R.id.nav_journal -> {
                    loadFragment(JournalMoodFragment())
                    binding.toolbar.title = item.title
                }
                R.id.nav_water -> {
                    loadFragment(WaterFragment())
                    binding.toolbar.title = item.title
                }
                R.id.nav_sleep -> {
                    loadFragment(LogSleepFragment())
                    binding.toolbar.title = item.title
                }
                R.id.nav_workout -> {
                    loadFragment(WorkoutTrackingFragment())
                    binding.toolbar.title = item.title
                }
                R.id.nav_period -> {
                    loadFragment(PeriodTrackerFragment())
                    binding.toolbar.title = item.title
                }
                R.id.nav_calorie -> {
                    loadFragment(CalorieFragment())
                    binding.toolbar.title = item.title
                }
                else -> {
                    clearFragment()
                    binding.toolbar.title = item.title
                }
            }

            item.isChecked = true
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun clearFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
    }

    private fun setupLogout() {
        val logoutBtn =
            binding.navigationView.findViewById<AppCompatButton>(R.id.btnLogout)

        logoutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    preferenceManager.clearUserData()
                    val intent = Intent(this, LogInActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}
