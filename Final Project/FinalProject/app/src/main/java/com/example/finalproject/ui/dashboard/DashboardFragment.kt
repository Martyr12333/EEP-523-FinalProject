package com.example.finalproject.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.finalproject.R
import com.example.finalproject.databinding.FragmentDashboardBinding
import com.example.finalproject.viewmodel.RideViewModel
import com.example.finalproject.data.LiveRideStats
import com.example.finalproject.viewmodel.RideUiState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.fragment.findNavController

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RideViewModel by viewModels()
    private var mapInitialized = false
    private var polyline: Polyline? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var currentLocation: GeoPoint? = null
    private lateinit var mapView: MapView

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                showLocationPermissionDeniedDialog()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        mapView = binding.map
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupUI()
        observeViewModel()
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        
        polyline = Polyline().apply {
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 5f
        }
        mapView.overlays.add(polyline)

        myLocationOverlay = MyLocationNewOverlay(mapView).apply {
            enableMyLocation()
            enableFollowLocation()
            isDrawAccuracyEnabled = true
        }
        mapView.overlays.add(myLocationOverlay)
    }

    private fun setupUI() {
        binding.btnStartRide.setOnClickListener {
            if (checkLocationPermission()) {
                viewModel.startRide()
            } else {
                requestLocationPermission()
            }
        }

        binding.btnPauseResume.setOnClickListener {
            if (viewModel.uiState.value?.isPaused == true) {
                viewModel.resumeRide()
            } else {
                viewModel.pauseRide()
            }
        }

        binding.btnStopRide.setOnClickListener {
            showStopRideConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.liveStats.collect { stats ->
                    updateStats(stats)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUIState(state)
                }
            }
        }
    }

    private fun updateStats(stats: LiveRideStats) {
        binding.tvDistance.text = String.format("%.2f km", stats.distance)
        binding.tvTime.text = formatDuration(stats.activeDuration)
        binding.tvSpeed.text = String.format("%.1f km/h", stats.currentSpeed * 3.6)
        binding.tvHeartRate.text = String.format("%d bpm", stats.currentHeartRate)
        
        if (stats.routePoints.isNotEmpty()) {
            val lastPoint = stats.routePoints.last()
            currentLocation = GeoPoint(lastPoint.latitude, lastPoint.longitude)
            updateMapLocation()
        }
    }

    private fun updateMapLocation() {
        currentLocation?.let { location ->
            mapView.controller.animateTo(location)
            myLocationOverlay?.setPersonHotspot(24f, 48f)
            myLocationOverlay?.enableMyLocation()
        }
    }

    private fun updateUIState(state: RideUiState) {
        binding.btnStartRide.isEnabled = !state.isRiding && !state.isPaused
        binding.btnPauseResume.isEnabled = state.isRiding || state.isPaused
        binding.btnStopRide.isEnabled = state.isRiding || state.isPaused

        // Update pause/resume button text and icon
        if (state.isPaused) {
            binding.btnPauseResume.text = getString(R.string.resume_ride)
            binding.btnPauseResume.setIconResource(R.drawable.ic_play)
        } else {
            binding.btnPauseResume.text = getString(R.string.pause_ride)
            binding.btnPauseResume.setIconResource(R.drawable.ic_pause)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        if (checkLocationPermission()) {
            myLocationOverlay?.enableMyLocation()
            mapView.invalidate()
        }
    }

    private fun showLocationPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_permission_required)
            .setMessage(R.string.location_permission_message)
            .setPositiveButton(R.string.settings) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showStopRideConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.stop_ride)
            .setMessage(R.string.stop_ride_confirmation)
            .setPositiveButton(R.string.stop) { dialog, _ ->
                viewModel.stopRide()
                dialog.dismiss()
                findNavController().navigate(R.id.nav_history)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
        _binding = null
    }
} 