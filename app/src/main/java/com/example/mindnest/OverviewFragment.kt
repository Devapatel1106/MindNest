package com.example.mindnest.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mindnest.DashboardActivity
import com.example.mindnest.R
import com.example.mindnest.data.QuoteDataSource
import com.example.mindnest.databinding.FragmentOverviewBinding
import com.example.mindnest.model.Feature
import com.example.mindnest.utils.PreferenceManager

class OverviewFragment : Fragment() {

    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    private val overviewViewModel: OverviewViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var quoteRunnable: Runnable? = null

    private lateinit var featureAdapter: FeatureAdapter
    private val features = mutableListOf<Feature>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startAutoQuote()
        setupFeatureGrid()
        observeSummaries()

        overviewViewModel.refreshAll()
    }

    override fun onResume() {
        super.onResume()

        overviewViewModel.refreshAll()
    }

    private fun setupFeatureGrid() {
        val preferenceManager = PreferenceManager(requireContext())
        val showPeriod = preferenceManager.getUserGender()?.lowercase() == "female"

        features.clear()
        features.addAll(getInitialFeatures().filter { it.title != "Period" || showPeriod })

        featureAdapter = FeatureAdapter(features) { feature ->
            (requireActivity() as? DashboardActivity)?.openModuleFromOverview(feature.title)
        }

        binding.recyclerViewFeatures.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = featureAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeSummaries() {

        overviewViewModel.taskSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Tasks", it)
        }

        overviewViewModel.waterSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Water", it)
        }

        overviewViewModel.journalSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Journal & Mood", it)
        }

        overviewViewModel.periodSummary.observe(viewLifecycleOwner) {
            if (features.any { f -> f.title == "Period" }) updateFeatureStat("Period", it)
        }

        overviewViewModel.sleepSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Sleep", it)
        }

        overviewViewModel.workoutSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Workout", it)
        }

        overviewViewModel.calorieSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Calories", it)
        }

        overviewViewModel.meditationSummary.observe(viewLifecycleOwner) {
            updateFeatureStat("Meditation", it)
        }
    }

    private fun updateFeatureStat(title: String, stat: String) {

        val index = features.indexOfFirst { it.title == title }

        if (index != -1) {
            val old = features[index]
            features[index] = old.copy(stat = stat)
            featureAdapter.notifyItemChanged(index)
        }
    }

    private fun startAutoQuote() {

        quoteRunnable = object : Runnable {
            override fun run() {
                _binding?.tvQuote?.text = QuoteDataSource.getRandomQuote()
                handler.postDelayed(this, 5000)
            }
        }

        handler.post(quoteRunnable!!)
    }

    private fun getInitialFeatures(): List<Feature> {

        return listOf(
            Feature(R.drawable.assignment_add_24px, "Tasks", "Manage daily goals", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.spa_24px, "Meditation", "Find your calm", "0 sessions", R.color.text_on_lavender),
            Feature(R.drawable.menu_book_24px, "Journal & Mood", "Reflect Feelings", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.menstrual_health_24px, "Period", "Cycle tracking", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.water_full_24px, "Water", "Stay hydrated", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.exercise_24px, "Workout", "Daily fitness", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.local_fire_department_24px, "Calories", "Monitor intake", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.airline_seat_individual_suite_24px, "Sleep", "Rest tracking", "Loading…", R.color.text_on_lavender)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quoteRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}
