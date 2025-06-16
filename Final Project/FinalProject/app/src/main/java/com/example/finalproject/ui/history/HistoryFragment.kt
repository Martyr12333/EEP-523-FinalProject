package com.example.finalproject.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.R
import com.example.finalproject.adapter.RecentRidesAdapter
import com.example.finalproject.data.Ride
import com.example.finalproject.databinding.FragmentHistoryBinding
import com.example.finalproject.viewmodel.RideViewModel
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val rideViewModel: RideViewModel by activityViewModels()
    private lateinit var ridesAdapter: RecentRidesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Setup rides recycler view
        ridesAdapter = RecentRidesAdapter(
            onRideClick = { ride -> navigateToRideDetails(ride) },
            onDeleteClick = { ride -> showDeleteConfirmationDialog(ride.id) }
        )
        
        binding.rvRideHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ridesAdapter
        }
    }
    
    private fun observeViewModel() {
        // Observe completed rides
        viewLifecycleOwner.lifecycleScope.launch {
            rideViewModel.completedRides.collect { rides ->
                ridesAdapter.submitList(rides)
                updateEmptyState(rides.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.rvRideHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyStateGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    
    private fun showDeleteConfirmationDialog(rideId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_ride)
            .setMessage(R.string.delete_ride_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteRide(rideId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deleteRide(rideId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                rideViewModel.deleteRide(rideId)
                Toast.makeText(context, R.string.ride_deleted, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, R.string.error_deleting_ride, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToRideDetails(ride: Ride) {
        val action = HistoryFragmentDirections.actionHistoryToRideDetails(ride.id)
        findNavController().navigate(action)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 