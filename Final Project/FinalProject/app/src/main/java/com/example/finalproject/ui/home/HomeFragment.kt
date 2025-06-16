package com.example.finalproject.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.R
import com.example.finalproject.adapter.RecentRidesAdapter
import com.example.finalproject.databinding.FragmentHomeBinding
import com.example.finalproject.viewmodel.RideViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val rideViewModel: RideViewModel by activityViewModels()
    private lateinit var recentRidesAdapter: RecentRidesAdapter
    
    private var isRequestingPermissions = false
    
    // Handle basic permissions first
    private val basicPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isRequestingPermissions = false
        
        // Log permission results
        permissions.forEach { (permission, granted) ->
            android.util.Log.d("HomeFragment", "Permission $permission: $granted")
        }
        
        // Check if location permissions are granted (essential for ride tracking)
        val locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        
        if (locationPermissionGranted) {
            android.util.Log.d("HomeFragment", "Location permission granted, starting ride")
            finishPermissionCheckAndStartRide()
        } else {
            android.util.Log.d("HomeFragment", "Location permission denied")
            Toast.makeText(context, "Location permission is required for ride tracking", Toast.LENGTH_LONG).show()
        }
    }
    
    // Handle background location permission separately
    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isRequestingPermissions = false
        android.util.Log.d("HomeFragment", "Background location permission: $granted")
        
        if (!granted) {
            Toast.makeText(context, "Background tracking will be limited when app is minimized", Toast.LENGTH_SHORT).show()
        }
        
        // Start ride regardless of background permission result
        startRide()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Setup recent rides recycler view
        recentRidesAdapter = RecentRidesAdapter(
            onRideClick = { ride ->
                // Navigate to ride details
                val action = HomeFragmentDirections.actionHomeToRideDetails(ride.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { ride ->
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(ride.id)
            }
        )
        
        binding.recyclerRecentRides.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentRidesAdapter
        }
        
        // Setup start ride button
        binding.btnStartRide.setOnClickListener {
            if (checkLocationPermission()) {
                rideViewModel.startRide()
                // Navigate to dashboard with proper navigation options
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_home, true)
                    .setLaunchSingleTop(true)
                    .build()
                findNavController().navigate(R.id.nav_dashboard, null, navOptions)
            } else {
                requestLocationPermission()
            }
        }
        
        // Setup navigation buttons
        binding.btnViewAllRides.setOnClickListener {
            findNavController().navigate(R.id.nav_history)
        }
    }
    
    private fun observeViewModel() {
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            rideViewModel.uiState.collect { state ->
                updateUIState(state)
            }
        }
        
        // Observe statistics
        viewLifecycleOwner.lifecycleScope.launch {
            rideViewModel.statistics.collect { stats ->
                updateStatistics(stats)
            }
        }
        
        // Observe recent rides
        viewLifecycleOwner.lifecycleScope.launch {
            rideViewModel.recentRides.collect { rides ->
                recentRidesAdapter.submitList(rides)
                binding.groupRecentRides.visibility = if (rides.isEmpty()) View.GONE else View.VISIBLE
                binding.tvNoRides.visibility = if (rides.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun updateUIState(state: com.example.finalproject.viewmodel.RideUiState) {
        binding.btnStartRide.isEnabled = !state.isStarting && !state.isRiding && !isRequestingPermissions
        
        when {
            state.isStarting -> {
                binding.tvStartRideText.text = "Starting..."
            }
            state.isRiding -> {
                // Navigate to dashboard when ride starts
                findNavController().navigate(R.id.nav_dashboard)
            }
            isRequestingPermissions -> {
                binding.tvStartRideText.text = "Requesting Permissions..."
            }
            else -> {
                binding.tvStartRideText.text = "Start Ride"
            }
        }
        
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            rideViewModel.clearError()
        }
    }
    
    private fun updateStatistics(stats: com.example.finalproject.viewmodel.OverallStatistics) {
        binding.apply {
            tvTotalDistance.text = stats.formattedTotalDistance
            tvTotalTime.text = stats.formattedTotalTime
            tvAvgSpeed.text = stats.formattedAverageSpeed
            tvTotalCalories.text = "${stats.totalCalories}"
            tvTotalRides.text = "${stats.totalRides}"
        }
    }
    
    private fun checkLocationPermission(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return locationPermission
    }
    
    private fun requestLocationPermission() {
        if (isRequestingPermissions) return
        
        isRequestingPermissions = true
        
        // Check location permission first
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            basicPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            return
        }
        
        // Check background location permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return
        }
        
        // Check Bluetooth permissions
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        
        val missingBluetoothPermissions = bluetoothPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingBluetoothPermissions.isNotEmpty()) {
            basicPermissionLauncher.launch(missingBluetoothPermissions.toTypedArray())
            return
        }
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            basicPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            return
        }
        
        // All permissions granted, start ride
        isRequestingPermissions = false
        startRide()
    }
    
    private fun finishPermissionCheckAndStartRide() {
        // Check if we have all required permissions
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasLocationPermission && hasBluetoothPermission) {
            isRequestingPermissions = false
            startRide()
        } else {
            isRequestingPermissions = false
            Toast.makeText(
                context,
                "Required permissions are not granted",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun startRide() {
        android.util.Log.d("HomeFragment", "Starting ride...")
        rideViewModel.startRide()
    }
    
    private fun showDeleteConfirmationDialog(rideId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_ride)
            .setMessage(R.string.delete_ride_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                rideViewModel.deleteRide(rideId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 