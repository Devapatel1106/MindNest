package com.example.mindnest.ui.water

import android.animation.ObjectAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindnest.R
import com.example.mindnest.WaterViewModel
import com.example.mindnest.databinding.BottomSheetAddWaterBinding
import com.example.mindnest.databinding.DialogSetTargetBinding
import com.example.mindnest.databinding.FragmentWaterBinding
import com.example.mindnest.ui.OverviewViewModel
import com.example.mindnest.utils.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class WaterFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentWaterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WaterViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    private val overviewViewModel: OverviewViewModel by activityViewModels()

    private val displayList = mutableListOf<WaterListItem>()
    private lateinit var adapter: WaterHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        binding.fabAddTask.setOnClickListener(this)
        binding.btnSetTarget.setOnClickListener(this)
        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadData()
    }

    private fun observeData() {
        viewModel.entries.observe(viewLifecycleOwner) {
            rebuildList()
            updateUI()
        }
        viewModel.dailyTarget.observe(viewLifecycleOwner) {
            updateUI()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.fabAddTask.id -> showAddWaterSheet()
            binding.btnSetTarget.id -> showSetTargetDialog()
        }
    }

    private fun showAddWaterSheet() {
        if ((viewModel.dailyTarget.value ?: 0) == 0) {
            Toast.makeText(requireContext(), getString(R.string.set_target_first), Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog)
        val b = BottomSheetAddWaterBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        b.btnAddWater.setOnClickListener {
            val amount = b.etWaterAmount.text.toString().toIntOrNull()
            if (amount == null || amount <= 0) {
                b.etWaterAmount.error = getString(R.string.invalid_amount)
                return@setOnClickListener
            }

            // Add water with callback after insertion
            viewModel.addWater(amount) {
                // This runs after water is successfully inserted
                overviewViewModel.notifyWaterChanged()
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSetTargetDialog() {
        val dialog = Dialog(requireContext(), R.style.TransparentBottomSheetDialog)
        val b = DialogSetTargetBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val currentTarget = viewModel.dailyTarget.value ?: 0
        if (currentTarget > 0) {
            b.etTarget.setText(currentTarget.toString())
        } else {
            b.etTarget.text = null
        }

        b.btnConfirmTarget.setOnClickListener {
            val target = b.etTarget.text.toString().toIntOrNull()
            if (target == null || target <= 0) {
                b.etTarget.error = getString(R.string.invalid_target)
                return@setOnClickListener
            }
            viewModel.setTarget(target)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateUI() {
        val target = viewModel.dailyTarget.value ?: 0
        val consumed = viewModel.todayTotal()

        if (target == 0) {
            binding.tvTargetValue.text = getString(R.string.set_target)
            binding.tvRemaining.visibility = View.GONE
            binding.tvProgressPercent.text = "0%"

            ObjectAnimator.ofInt(
                binding.progressWater,
                "progress",
                binding.progressWater.progress,
                0
            ).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            return
        }

        val progress = ((consumed.toFloat() / target) * 100)
            .toInt()
            .coerceIn(0, 100)

        val remaining = (target - consumed).coerceAtLeast(0)

        binding.tvTargetValue.text = getString(R.string.target_ml, target)
        binding.tvRemaining.visibility = View.VISIBLE
        binding.tvRemaining.text = getString(R.string.remaining_ml, remaining)
        binding.tvProgressPercent.text = "$progress%"

        if (binding.progressWater.progress != progress) {
            ObjectAnimator.ofInt(
                binding.progressWater,
                "progress",
                binding.progressWater.progress,
                progress
            ).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun rebuildList() {
        displayList.clear()

        viewModel.entries.value
            ?.groupBy { it.date }
            ?.toSortedMap(compareByDescending { it })
            ?.forEach { (date, entries) ->

                displayList.add(
                    WaterListItem.DateHeader(
                        date,
                        viewModel.isTargetAchieved(date)
                    )
                )

                entries.asReversed().forEach {
                    displayList.add(WaterListItem.WaterLog(it))
                }
            }

        adapter.notifyDataSetChanged()
    }

    private fun setupRecycler() {
        adapter = WaterHistoryAdapter(displayList)
        binding.rvWaterHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWaterHistory.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}