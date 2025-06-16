package com.example.finalproject.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.LiveRideStats
import com.example.finalproject.data.Ride
import com.example.finalproject.data.RideEntity
import com.example.finalproject.data.RideRepository
import com.example.finalproject.service.RideTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RideViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = RideRepository(application)
    
    private var trackingService: RideTrackingService? = null
    private var isServiceBound = false
    
    // UI State
    private val _uiState = MutableStateFlow(RideUiState())
    val uiState: StateFlow<RideUiState> = _uiState.asStateFlow()
    
    // Live ride stats from service
    private val _liveStats = MutableStateFlow(LiveRideStats())
    val liveStats: StateFlow<LiveRideStats> = _liveStats.asStateFlow()
    
    // Ride history
    private val _recentRides = MutableStateFlow<List<Ride>>(emptyList())
    val recentRides: StateFlow<List<Ride>> = _recentRides.asStateFlow()
    
    // All completed rides for history view
    private val _completedRides = MutableStateFlow<List<Ride>>(emptyList())
    val completedRides: StateFlow<List<Ride>> = _completedRides.asStateFlow()
    
    // Statistics
    private val _statistics = MutableStateFlow(OverallStatistics())
    val statistics: StateFlow<OverallStatistics> = _statistics.asStateFlow()
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RideTrackingService.RideTrackingBinder
            trackingService = binder.getService()
            isServiceBound = true
            
            // Observe service stats
            viewModelScope.launch {
                trackingService?.rideStats?.collect { stats ->
                    Log.d("RideViewModel", "Received stats update: duration=${stats.duration}, activeDuration=${stats.activeDuration}")
                    _liveStats.value = stats
                    _uiState.value = _uiState.value.copy(
                        isRiding = stats.isRecording,
                        isPaused = stats.isPaused
                    )
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
        }
    }
    
    init {
        loadRecentRides()
        loadStatistics()
        loadCompletedRides()
        checkForActiveRide()
    }
    
    fun startRide() {
        _uiState.value = _uiState.value.copy(isStarting = true)
        
        // Start the tracking service
        RideTrackingService.startService(getApplication())
        
        // Bind to service
        val intent = Intent(getApplication(), RideTrackingService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        _uiState.value = _uiState.value.copy(
            isStarting = false,
            isRiding = true,
            isPaused = false
        )
    }
    
    fun pauseRide() {
        if (_uiState.value.isRiding && !_uiState.value.isPaused) {
            RideTrackingService.pauseService(getApplication())
            _uiState.value = _uiState.value.copy(isPaused = true)
        }
    }
    
    fun resumeRide() {
        if (_uiState.value.isRiding && _uiState.value.isPaused) {
            RideTrackingService.resumeService(getApplication())
            _uiState.value = _uiState.value.copy(isPaused = false)
        }
    }
    
    fun stopRide() {
        _uiState.value = _uiState.value.copy(isStopping = true)
        
        RideTrackingService.stopService(getApplication())
        
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
        
        _uiState.value = _uiState.value.copy(
            isRiding = false,
            isPaused = false,
            isStopping = false
        )
        
        // Refresh data
        loadRecentRides()
        loadStatistics()
        loadCompletedRides()
    }
    
    fun cancelRide() {
        _uiState.value = _uiState.value.copy(isStopping = true)
        
        RideTrackingService.stopService(getApplication())
        
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
        
        viewModelScope.launch {
            repository.cancelActiveRides()
            repository.deleteCancelledRides()
        }
        
        _uiState.value = _uiState.value.copy(
            isRiding = false,
            isPaused = false,
            isStopping = false
        )
        
        // Refresh data
        loadRecentRides()
        loadStatistics()
        loadCompletedRides()
    }
    
    fun deleteRide(rideId: Long) {
        viewModelScope.launch {
            repository.deleteRideById(rideId)
            loadRecentRides()
            loadStatistics()
            loadCompletedRides()
        }
    }
    
    private fun loadRecentRides() {
        viewModelScope.launch {
            repository.getRecentRides(10).map { entities ->
                entities.map { it.toRide() }
            }.collect { rides ->
                _recentRides.value = rides
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            // Collect all statistics
            launch {
                repository.getTotalDistance().collect { distance ->
                    _statistics.value = _statistics.value.copy(totalDistance = distance)
                }
            }
            launch {
                repository.getTotalActiveTime().collect { time ->
                    _statistics.value = _statistics.value.copy(totalActiveTime = time)
                }
            }
            launch {
                repository.getAverageSpeed().collect { speed ->
                    _statistics.value = _statistics.value.copy(averageSpeed = speed)
                }
            }
            launch {
                repository.getTotalCalories().collect { calories ->
                    _statistics.value = _statistics.value.copy(totalCalories = calories)
                }
            }
            launch {
                repository.getCompletedRidesCount().collect { count ->
                    _statistics.value = _statistics.value.copy(totalRides = count)
                }
            }
        }
    }
    
    private fun loadCompletedRides() {
        viewModelScope.launch {
            repository.getCompletedRides().map { entities ->
                entities.map { it.toRide() }
            }.collect { rides ->
                _completedRides.value = rides
            }
        }
    }
    
    private fun checkForActiveRide() {
        viewModelScope.launch {
            val activeRide = repository.getActiveRide()
            if (activeRide != null) {
                // There's an active ride, but service isn't running
                // Mark it as cancelled and clean up
                repository.cancelActiveRides()
                repository.deleteCancelledRides()
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
        }
    }
}

data class RideUiState(
    val isRiding: Boolean = false,
    val isPaused: Boolean = false,
    val isStarting: Boolean = false,
    val isStopping: Boolean = false,
    val error: String? = null
)

data class OverallStatistics(
    val totalDistance: Double = 0.0,
    val totalActiveTime: Long = 0L,
    val averageSpeed: Double = 0.0,
    val totalCalories: Int = 0,
    val totalRides: Int = 0
) {
    val formattedTotalDistance: String
        get() = String.format("%.1f km", totalDistance)
    
    val formattedTotalTime: String
        get() {
            val hours = totalActiveTime / 3600000
            val minutes = (totalActiveTime % 3600000) / 60000
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    
    val formattedAverageSpeed: String
        get() = String.format("%.1f km/h", averageSpeed)
}

// Extension function to convert RideEntity to Ride
private fun RideEntity.toRide(): Ride {
    return Ride(
        id = id,
        startTime = startTime,
        endTime = endTime,
        duration = duration,
        pausedDuration = pausedDuration,
        distance = distance,
        averageSpeed = averageSpeed,
        maxSpeed = maxSpeed,
        calories = calories,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        routePoints = routePoints,
        status = status
    )
} 