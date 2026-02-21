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
import com.example.mindnest.databinding.DialogMindscoreGraphBinding
import com.example.mindnest.databinding.FragmentOverviewBinding
import com.example.mindnest.model.Feature
import com.example.mindnest.utils.PreferenceManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class OverviewFragment : Fragment() {

    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    private val overviewViewModel: OverviewViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var quoteRunnable: Runnable? = null
    private var greetingRunnable: Runnable? = null

    private lateinit var featureAdapter: FeatureAdapter
    private val features = mutableListOf<Feature>()

    private var midnightRunnable: Runnable? = null

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

        setupGreeting()
        setupGreetingAutoUpdate()
        startAutoQuote()
        setupFeatureGrid()
        observeSummaries()
        observeMindScore()
        setupMindScoreCardClick()

        overviewViewModel.refreshAll()

        scheduleMidnightRefresh()
    }

    private fun scheduleMidnightRefresh() {

        val now = Calendar.getInstance()

        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = midnight.timeInMillis - now.timeInMillis

        midnightRunnable = Runnable {
            if (_binding != null) {
                overviewViewModel.refreshAll()
            }
            scheduleMidnightRefresh()
        }

        handler.postDelayed(midnightRunnable!!, delay)
    }

    private fun setupGreeting() {

        val preferenceManager = PreferenceManager(requireContext())
        val userName = preferenceManager.getUserName() ?: "User"

        val calendar = java.util.Calendar.getInstance()
        val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        val greeting = when {
            hourOfDay < 12 -> "Good morning"
            hourOfDay < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        binding.tvGreeting.text = "$greeting, $userName 👋"
    }


    private fun setupGreetingAutoUpdate() {

        greetingRunnable = object : Runnable {
            override fun run() {
                if (_binding != null) {
                    setupGreeting()
                    handler.postDelayed(this, 60_000)
                }
            }
        }

        handler.post(greetingRunnable!!)
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

    private fun observeMindScore() {
        overviewViewModel.mindScore.observe(viewLifecycleOwner) { score ->
            animateProgressBar(score)
            animateScoreText(score)
        }

        overviewViewModel.mindScoreStatus.observe(viewLifecycleOwner) { status ->
            binding.tvMindStatus.text = status.ifEmpty { "—" }
        }
    }

    private fun animateProgressBar(score: Int) {
        val animator = android.animation.ObjectAnimator.ofInt(
            binding.progressMindScore,
            "progress",
            0,
            score
        )
        animator.duration = 1000
        animator.start()
    }

    private fun animateScoreText(score: Int) {
        val animator = android.animation.ValueAnimator.ofInt(0, score)
        animator.duration = 1000

        animator.addUpdateListener {
            binding.tvMindScore.text = (it.animatedValue as Int).toString()
        }

        animator.start()
    }

    private fun setupMindScoreCardClick() {
        binding.mindScoreCard.setOnClickListener {
            showMindScoreGraphDialog()
        }
    }

    private fun showMindScoreGraphDialog() {
        val dialogBinding = DialogMindscoreGraphBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        setupLineChart(dialogBinding)

        dialogBinding.btnCloseGraph.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupLineChart(binding: DialogMindscoreGraphBinding) {
        val lineChart = binding.lineChartMindScore

        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setDragEnabled(true)
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setDrawGridBackground(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textSize = 10f

        val leftAxis = lineChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.setDrawGridLines(true)
        leftAxis.textSize = 10f

        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false

        lineChart.legend.isEnabled = false

        overviewViewModel.getLast7DaysMindScores().observe(viewLifecycleOwner) { scores ->
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>()

            scores.forEachIndexed { index, (date, score) ->
                entries.add(Entry(index.toFloat(), score.toFloat()))
                labels.add(date)
            }

            xAxis.valueFormatter =
                object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < labels.size) {
                            labels[index]
                        } else {
                            ""
                        }
                    }
                }

            val dataSet = LineDataSet(entries, "Mind Score")
            dataSet.color = requireContext().getColor(R.color.lavender_dark)
            dataSet.setCircleColor(requireContext().getColor(R.color.lavender_dark))
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 4f
            dataSet.setDrawCircleHole(false)
            dataSet.valueTextSize = 10f
            dataSet.setDrawValues(true)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.cubicIntensity = 0.2f

            val lineData = LineData(dataSet)
            lineChart.data = lineData
            lineChart.invalidate()
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
            Feature(R.drawable.water_full_24px, "Water", "Stay hydrated", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.exercise_24px, "Workout", "Daily fitness", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.spa_24px, "Meditation", "Find your calm", "0 sessions", R.color.text_on_lavender),
            Feature(R.drawable.menu_book_24px, "Journal & Mood", "Reflect feelings", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.airline_seat_individual_suite_24px, "Sleep", "Rest tracking", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.local_fire_department_24px, "Calories", "Monitor intake", "Loading…", R.color.text_on_lavender),
            Feature(R.drawable.menstrual_health_24px, "Period", "Cycle tracking", "Loading…", R.color.text_on_lavender)
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        quoteRunnable?.let { handler.removeCallbacks(it) }
        greetingRunnable?.let { handler.removeCallbacks(it) }
        midnightRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }


}
