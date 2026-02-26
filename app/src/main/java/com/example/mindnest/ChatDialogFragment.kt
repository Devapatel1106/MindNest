package com.example.mindnest.ui.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindnest.R
import com.example.mindnest.databinding.FragmentChatDialogBinding
import com.example.mindnest.data.ChatBotContext
import com.example.mindnest.data.ChatBotEngine
import com.example.mindnest.ui.OverviewViewModel
import com.example.mindnest.utils.PreferenceManager

class ChatDialogFragment : DialogFragment() {

    private var _binding: FragmentChatDialogBinding? = null
    private val binding get() = _binding!!

    private val chatViewModel: ChatViewModel by activityViewModels()
    private val overviewViewModel: OverviewViewModel by activityViewModels()

    private lateinit var adapter: ChatAdapter
    private var hasTriggeredWelcome = false

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {

            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setGravity(Gravity.CENTER)

            setBackgroundDrawableResource(android.R.color.transparent)

            attributes?.windowAnimations = R.style.DialogAnimation
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = ChatAdapter(emptyList())

        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatDialogFragment.adapter
            setHasFixedSize(true)
        }

        observeMessages()

        binding.btnSend.setOnClickListener {

            val text = binding.etMessage.text.toString().trim()

            if (text.isNotEmpty()) {

                chatViewModel.sendMessage(text, true)

                binding.etMessage.text?.clear()

                val prefs = PreferenceManager(requireContext())

                val botContext = ChatBotContext(
                    userName = prefs.getUserName().orEmpty(),
                    mindScore = overviewViewModel.mindScore.value ?: 0,
                    mindScoreStatus = overviewViewModel.mindScoreStatus.value.orEmpty(),
                    taskSummary = overviewViewModel.taskSummary.value.orEmpty(),
                    waterSummary = overviewViewModel.waterSummary.value.orEmpty(),
                    sleepSummary = overviewViewModel.sleepSummary.value.orEmpty(),
                    workoutSummary = overviewViewModel.workoutSummary.value.orEmpty(),
                    journalSummary = overviewViewModel.journalSummary.value.orEmpty(),
                    periodSummary = overviewViewModel.periodSummary.value.orEmpty(),
                    calorieSummary = overviewViewModel.calorieSummary.value.orEmpty(),
                    meditationSummary = overviewViewModel.meditationSummary.value.orEmpty()
                )

                val reply = ChatBotEngine.getReply(text, requireContext(), botContext)

                // Small delay for natural chatbot feel
                binding.rvChat.postDelayed({
                    chatViewModel.sendMessage(reply, false)
                }, 400)
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun observeMessages() {

        chatViewModel.messages.observe(viewLifecycleOwner) { list ->

            val uiList = list?.map {
                ChatMessage(
                    it.message,
                    it.isUser
                )
            } ?: emptyList()

            adapter.updateMessages(uiList)

            if (uiList.isEmpty() && !hasTriggeredWelcome) {
                hasTriggeredWelcome = true
                chatViewModel.sendMessage(
                    "Hi! I'm your MindNest assistant. How can I help you today?",
                    false
                )
            }

            if (uiList.isNotEmpty()) {
                binding.rvChat.smoothScrollToPosition(uiList.size - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}