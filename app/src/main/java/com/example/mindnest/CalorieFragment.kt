package com.example.mindnest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.adapter.FoodAdapter
import com.example.calorietracker.data.FoodDataSource
import com.example.calorietracker.model.FoodItem
import com.example.mindnest.R
import com.example.mindnest.databinding.FragmentCalorieBinding
import com.example.mindnest.viewmodel.CalorieViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CalorieFragment : Fragment() {

    private var _binding: FragmentCalorieBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalorieViewModel by activityViewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private lateinit var foodAdapter: FoodAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalorieBinding.inflate(inflater, container, false)

        setupFoodRecycler()
        setupObservers()
        setupClicks()

        return binding.root
    }


    private fun setupObservers() {

        viewModel.userInfo.observe(viewLifecycleOwner) { info ->
            binding.tvWeight.text = "Weight: ${info.weight} kg"
            binding.tvHeight.text = "Height: ${info.height} cm"
            binding.tvAge.text = "Age: ${info.age}"
            binding.tvTargetCalories.text = info.targetCalories.toString()

            // 🔥 sync gender UI
            updateGenderUI(info.gender)
        }

        viewModel.consumedCalories.observe(viewLifecycleOwner) { consumed ->
            binding.tvConsumed.text = "Consumed: $consumed kcal"

            val target = viewModel.userInfo.value?.targetCalories ?: 2000
            binding.calorieProgress.progress = ((consumed * 100) / target).coerceIn(0, 100)
        }

        viewModel.remainingCalories.observe(viewLifecycleOwner) { remaining ->
            binding.tvRemaining.text =
                if (remaining >= 0) "Remaining: $remaining kcal"
                else "Exceeded by ${-remaining} kcal"
        }

        viewModel.suggestion.observe(viewLifecycleOwner) {
            binding.tvSuggestion.text = it
        }

        viewModel.foodList.observe(viewLifecycleOwner) {
            foodAdapter.submitList(it.toList())
        }
    }

    private fun setupClicks() {

        binding.tvWeight.setOnClickListener { showUserInfoDialog() }
        binding.tvHeight.setOnClickListener { showUserInfoDialog() }
        binding.tvAge.setOnClickListener { showUserInfoDialog() }

        binding.btnMale.setOnClickListener { updateGender("Male") }
        binding.btnFemale.setOnClickListener { updateGender("Female") }

        binding.btnPlusTarget.setOnClickListener { viewModel.increaseTarget() }
        binding.btnMinusTarget.setOnClickListener { viewModel.decreaseTarget() }

        binding.btnAddFood.setOnClickListener { showFoodSearchPopup() }

        binding.btnResetFood.setOnClickListener {
            viewModel.clearFoodList()
            Toast.makeText(requireContext(), "Calories reset", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showUserInfoDialog() {

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_user_info, null)

        val etWeight = dialogView.findViewById<AppCompatEditText>(R.id.etWeight)
        val etHeight = dialogView.findViewById<AppCompatEditText>(R.id.etHeight)
        val etAge = dialogView.findViewById<AppCompatEditText>(R.id.etAge)

        val current = viewModel.userInfo.value
        current?.let {
            etWeight.setText(it.weight.toString())
            etHeight.setText(it.height.toString())
            etAge.setText(it.age.toString())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->

                val weight = etWeight.text.toString().toIntOrNull()
                val height = etHeight.text.toString().toIntOrNull()
                val age = etAge.text.toString().toIntOrNull()

                if (weight == null || height == null || age == null) {
                    Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val gender = current?.gender ?: "Male"
                viewModel.updateUserInfo(weight, height, age, gender)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun updateGender(gender: String) {
        val info = viewModel.userInfo.value ?: return
        if (info.gender == gender) return

        viewModel.updateUserInfo(
            info.weight,
            info.height,
            info.age,
            gender
        )
    }

    private fun updateGenderUI(gender: String) {
        if (gender == "Male") {
            binding.btnMale.setBackgroundResource(R.drawable.bg_gender_selected)
            binding.btnFemale.setBackgroundResource(R.drawable.bg_gender_unselected)
        } else {
            binding.btnFemale.setBackgroundResource(R.drawable.bg_gender_selected)
            binding.btnMale.setBackgroundResource(R.drawable.bg_gender_unselected)
        }
    }


    private fun setupFoodRecycler() {
        foodAdapter = FoodAdapter { food ->
            viewModel.addFood(food)
            Toast.makeText(requireContext(), "${food.name} added", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerFood.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foodAdapter
        }
    }

    private fun showFoodSearchPopup() {

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_food_search, null)

        val etSearch = dialogView.findViewById<AppCompatEditText>(R.id.etSearchFood)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.recyclerFoodSearch)

        val fullFoodList = FoodDataSource.foodList.toList()

        val dialogAdapter = FoodAdapter { food ->
            viewModel.addFood(food)
            Toast.makeText(requireContext(), "${food.name} added", Toast.LENGTH_SHORT).show()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = dialogAdapter
        dialogAdapter.submitList(fullFoodList)

        etSearch.addTextChangedListener { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()

            val filtered = if (query.isEmpty()) {
                fullFoodList
            } else {
                fullFoodList.filter {
                    it.name.lowercase().contains(query)
                }
            }

            dialogAdapter.submitList(filtered.toList())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Food")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
