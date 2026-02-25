package com.example.mindnest.ui.mindfulness

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindnest.PastSession
import com.example.mindnest.PastSessionAdapter
import com.example.mindnest.PastSessionViewModel
import com.example.mindnest.R
import com.example.mindnest.databinding.FragmentMindfulnessSessionBinding
import com.example.mindnest.utils.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MindfulnessSessionFragment : Fragment() {

    private var _binding: FragmentMindfulnessSessionBinding? = null
    private val binding get() = _binding!!

    private var totalMillis = 0L
    private var millisRemaining = 0L
    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false

    private var sessionStartTime: Long = 0L
    private var mediaPlayer: MediaPlayer? = null
    private var sessionSaved = false

    private val adapter = PastSessionAdapter()
    private val sessionViewModel: PastSessionViewModel by activityViewModels()

    private var userId: Long = -1L
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMindfulnessSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        userId = preferenceManager.getUserId()

        if (userId <= 0) {
            userId = requireActivity()
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getLong("user_id", -1L)
        }

        if (userId <= 0) {
            return
        }

        activity?.findViewById<View>(R.id.toolbar)?.isVisible = false
        binding.btnBack.setOnClickListener { navigateBack() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    saveOngoingSession()
                    navigateBack()
                }
            }
        )

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                activity?.findViewById<View>(R.id.toolbar)?.isVisible = true
            }
        })

        val minutes = arguments?.getInt("SESSION_MINUTES") ?: 5
        totalMillis = minutes * 60 * 1000L
        millisRemaining = totalMillis

        setupRecycler()
        setupClicks()
        updateTimerText(millisRemaining)
        initAudio()

        sessionViewModel.loadSessions(requireContext(), userId)

        sessionViewModel.listenForRealtimeUpdates(requireContext(), userId)

        sessionViewModel.pastSessions.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
            if (list.isNotEmpty()) binding.rvPastSessions.scrollToPosition(0)
        }
    }

    private fun navigateBack() {
        activity?.findViewById<View>(R.id.toolbar)?.isVisible = true
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, FragmentMindfulness())
        }
    }

    private fun setupRecycler() {
        binding.rvPastSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastSessions.adapter = adapter
    }

    private fun setupClicks() = with(binding) {
        btnStart.setOnClickListener { startTimer() }
        btnPause.setOnClickListener { pauseTimer() }
        btnSave.setOnClickListener { saveOngoingSession() }
    }

    private fun initAudio() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.arnor_chosic).apply {
                isLooping = true
                setVolume(0.6f, 0.6f)
            }
        }
    }

    private fun startTimer() {
        if (isRunning) return

        isRunning = true
        sessionSaved = false
        sessionStartTime = System.currentTimeMillis()

        initAudio()
        mediaPlayer?.start()

        countDownTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                millisRemaining = millisUntilFinished
                updateTimerText(millisRemaining)
                updateProgress()
            }

            override fun onFinish() {
                isRunning = false
                millisRemaining = 0L
                updateTimerText(0L)
                updateProgress()
                stopAudio()
                saveOngoingSession()
            }
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        mediaPlayer?.pause()
    }

    private fun saveOngoingSession() {
        countDownTimer?.cancel()
        isRunning = false
        stopAudio()
        autoSaveSession()
    }

    private fun autoSaveSession() {
        if (sessionSaved || millisRemaining == totalMillis) return
        sessionSaved = true

        val sessionEndTime = System.currentTimeMillis()
        val elapsedMillis = sessionEndTime - sessionStartTime

        val minutes = elapsedMillis / 1000 / 60
        val seconds = elapsedMillis / 1000 % 60
        val duration = String.format("%d:%02d min", minutes, seconds)

        val newSession = PastSession(
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(sessionStartTime)),
            date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(sessionStartTime)),
            duration = duration,
            startMillis = sessionStartTime
        )

        sessionViewModel.addSession(newSession, userId, requireContext())
    }

    private fun updateTimerText(millis: Long) {
        val min = millis / 1000 / 60
        val sec = millis / 1000 % 60
        binding.tvTimer.text = String.format("%02d:%02d", min, sec)
    }

    private fun updateProgress() {
        val progress =
            ((totalMillis - millisRemaining).toFloat() / totalMillis * 100).roundToInt()
        binding.circularProgress.progress = progress
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepareAsync()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRunning) saveOngoingSession()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
        activity?.findViewById<View>(R.id.toolbar)?.isVisible = true
    }
}