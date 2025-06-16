package com.example.finalproject.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finalproject.R
import com.example.finalproject.databinding.FragmentRideDetailsBinding
import com.example.finalproject.viewmodel.RideViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RideDetailsFragment : Fragment() {
    
    private var _binding: FragmentRideDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RideViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRideDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadRideDetails()
    }
    
    private fun setupUI() {
        // Setup back button
        binding.toolbar.setNavigationOnClickListener {
            // Use the action to navigate back to history
            findNavController().navigate(R.id.action_ride_details_to_history)
        }
    }
    
    private fun loadRideDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ride = viewModel.completedRides.first().find { it.id == arguments?.getLong("rideId") }
            ride?.let { displayRideDetails(it) }
        }
    }
    
    private fun displayRideDetails(ride: com.example.finalproject.data.Ride) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        binding.apply {
            tvRideDate.text = dateFormat.format(ride.startTime)
            tvDuration.text = formatDuration(ride.duration)
            tvDistance.text = String.format("%.2f km", ride.distance)
            tvAverageSpeed.text = String.format("%.1f km/h", ride.averageSpeed)
            tvMaxSpeed.text = String.format("%.1f km/h", ride.maxSpeed)
            tvCalories.text = "${ride.calories} kcal"
            
            if (ride.averageHeartRate > 0) {
                tvAverageHeartRate.text = "${ride.averageHeartRate} bpm"
                tvMaxHeartRate.text = "${ride.maxHeartRate} bpm"
            } else {
                tvAverageHeartRate.text = "N/A"
                tvMaxHeartRate.text = "N/A"
            }
        }
    }
    
    private fun formatDuration(durationMs: Long): String {
        val hours = durationMs / 3600000
        val minutes = (durationMs % 3600000) / 60000
        val seconds = (durationMs % 60000) / 1000
        
        return when {
            hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 