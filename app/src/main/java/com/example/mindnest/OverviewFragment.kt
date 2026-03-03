package com.example.mindnest.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mindnest.DashboardActivity
import com.example.mindnest.R
import com.example.mindnest.data.QuoteDataSource
import com.example.mindnest.databinding.DialogMindscoreGraphBinding
import com.example.mindnest.databinding.FragmentOverviewBinding
import com.example.mindnest.model.Feature
import com.example.mindnest.utils.PdfReportGenerator
import com.example.mindnest.utils.PreferenceManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.example.mindnest.ui.chat.ChatDialogFragment

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

        observeUserGreeting()
        setupGreetingAutoUpdate()
        startAutoQuote()
        setupFeatureGrid()
        observeSummaries()
        observeMindScore()
        setupMindScoreCardClick()
        observeWeeklyPerformance()
        setupDownloadReport()
        overviewViewModel.refreshAll()

        scheduleMidnightRefresh()

        val pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.fab_pulse)
        binding.fabChatbot.startAnimation(pulse)

        binding.fabChatbot.setOnClickListener {
            val dialog = ChatDialogFragment()
            dialog.show(parentFragmentManager, "ChatDialog")
        }
    }

    private fun observeUserGreeting() {

        overviewViewModel.userName.observe(viewLifecycleOwner) { name ->

            val calendar = Calendar.getInstance()
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

            val greeting = when {
                hourOfDay in 0..4 -> "It's time to sleep"
                hourOfDay in 5..11 -> "Good morning"
                hourOfDay in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }

            binding.tvGreeting.text = "$greeting, ${name ?: "User"} 👋"
        }
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

    private fun setupGreetingAutoUpdate() {

        greetingRunnable = object : Runnable {
            override fun run() {
                if (_binding != null) {

                    val calendar = Calendar.getInstance()
                    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

                    val greeting = when {
                        hourOfDay in 0..4 -> "It's time to sleep"
                        hourOfDay in 5..11 -> "Good morning"
                        hourOfDay in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    }

                    val name = overviewViewModel.userName.value ?: "User"
                    binding.tvGreeting.text = "$greeting, $name 👋"

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

        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = when {
            screenWidthDp < 360 -> 1
            screenWidthDp < 600 -> 2
            screenWidthDp < 840 -> 3
            else -> 4
        }

        binding.recyclerViewFeatures.apply {
            layoutManager = GridLayoutManager(requireContext(), spanCount)
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

        configureLineChart(dialogBinding.lineChartMindScore)

        overviewViewModel.getLast7DaysMindScores().observe(viewLifecycleOwner) { scores ->
            updateChartData(dialogBinding.lineChartMindScore, scores)
        }

        dialogBinding.btnCloseGraph.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun configureLineChart(lineChart: LineChart) {
        lineChart.description.isEnabled = false
        lineChart.setNoDataText("")
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
        leftAxis.axisMaximum = 110f
        leftAxis.setDrawGridLines(true)
        leftAxis.textSize = 10f

        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false

        lineChart.legend.isEnabled = false
    }

    private fun updateChartData(lineChart: LineChart, scores: List<Pair<String, Int>>) {
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        scores.forEachIndexed { index, (date, score) ->
            entries.add(Entry(index.toFloat(), score.toFloat()))
            labels.add(date)
        }

        lineChart.xAxis.valueFormatter =
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
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
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

    private fun observeWeeklyPerformance() {

        overviewViewModel.weeklyAverage.observe(viewLifecycleOwner) {
            binding.tvWeeklyAverage.text = it.toString()
        }

        overviewViewModel.weeklyMeta.observe(viewLifecycleOwner) {
            binding.tvWeeklyMeta.text = it
        }

        overviewViewModel.weeklyInsight.observe(viewLifecycleOwner) {
            binding.tvWeeklyInsight.text = it
        }
    }

    private fun setupDownloadReport() {
        binding.btnDownloadReport.setOnClickListener {
            generatePdfReport()
        }
    }

    private fun generatePdfReport() {
        val userName = overviewViewModel.userName.value ?: "User"
        val mindScore = overviewViewModel.mindScore.value ?: 0
        val avgScore = overviewViewModel.weeklyAverage.value ?: 0
        val highScore = overviewViewModel.weeklyHigh.value ?: 0
        val lowScore = overviewViewModel.weeklyLow.value ?: 0
        val insight = overviewViewModel.weeklyInsight.value ?: ""

        val moduleData = mutableMapOf<String, String>()
        overviewViewModel.taskSummary.value?.let { moduleData["Tasks"] = it }
        overviewViewModel.waterSummary.value?.let { moduleData["Water"] = it }
        overviewViewModel.workoutSummary.value?.let { moduleData["Workout"] = it }
        overviewViewModel.meditationSummary.value?.let { moduleData["Meditation"] = it }
        overviewViewModel.journalSummary.value?.let { moduleData["Journal & Mood"] = it }
        overviewViewModel.sleepSummary.value?.let { moduleData["Sleep"] = it }
        overviewViewModel.calorieSummary.value?.let { moduleData["Calories"] = it }

        val preferenceManager = PreferenceManager(requireContext())
        if (preferenceManager.getUserGender()?.lowercase() == "female") {
            overviewViewModel.periodSummary.value?.let { moduleData["Period"] = it }
        }

        lifecycleScope.launch {
            Toast.makeText(requireContext(), "Generating report...", Toast.LENGTH_SHORT).show()

            // Create a hidden chart to capture as bitmap
            val chart = LineChart(requireContext())
            chart.layout(0, 0, 800, 400) // Fixed size for bitmap
            configureLineChart(chart)

            // Fetch data synchronously for the report
            val scores = overviewViewModel.fetchLast7DaysMindScoresSync()
            updateChartData(chart, scores)

            val bitmap = withContext(Dispatchers.Default) {
                val b = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
                val c = Canvas(b)
                c.drawColor(android.graphics.Color.WHITE)
                chart.draw(c)
                b
            }

            val uri = withContext(Dispatchers.IO) {
                PdfReportGenerator.generateWellnessReport(
                    requireContext(), userName, mindScore, avgScore, highScore, lowScore, insight, bitmap, moduleData
                )
            }

            if (uri != null) {
                Toast.makeText(requireContext(), "Report saved to Downloads", Toast.LENGTH_LONG).show()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Open Report"))
            } else {
                Toast.makeText(requireContext(), "Failed to generate report", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quoteRunnable?.let { handler.removeCallbacks(it) }
        greetingRunnable?.let { handler.removeCallbacks(it) }
        midnightRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}