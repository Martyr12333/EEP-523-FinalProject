package com.example.finalproject.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.example.finalproject.data.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTracker(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private var locationCallback: LocationCallback? = null
    
    // State tracking
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation
    
    private val _currentSpeed = MutableStateFlow(0.0)
    val currentSpeed: StateFlow<Double> = _currentSpeed
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking
    
    private val routePoints = mutableListOf<RoutePoint>()
    private var totalDistance = 0.0
    private var lastLocation: Location? = null
    
    private var locationListener: LocationListener? = null
    
    interface LocationListener {
        fun onLocationUpdate(location: Location, speed: Double)
        fun onDistanceUpdate(totalDistance: Double)
    }
    
    fun setLocationListener(listener: LocationListener?) {
        this.locationListener = listener
    }
    
    fun startTracking() {
        if (!hasLocationPermission()) {
            return
        }
        
        if (_isTracking.value) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(100L)
            .setMaxUpdateDelayMillis(1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
    
    fun stopTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
        _isTracking.value = false
    }
    
    private fun updateLocation(location: Location) {
        _currentLocation.value = location
        
        // Calculate speed (convert m/s to km/h)
        val speedKmh = if (location.hasSpeed()) {
            (location.speed * 3.6).coerceAtLeast(0.0)
        } else {
            0.0
        }
        _currentSpeed.value = speedKmh
        
        // Calculate distance if we have a previous location
        lastLocation?.let { prevLocation ->
            val distance = prevLocation.distanceTo(location) / 1000.0 // Convert to km
            totalDistance += distance
            Log.d("LocationTracker", "Distance update: added=${String.format("%.3f", distance)}km, total=${String.format("%.3f", totalDistance)}km")
            locationListener?.onDistanceUpdate(totalDistance)
        }
        
        // Add route point
        val routePoint = RoutePoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            speed = speedKmh
        )
        routePoints.add(routePoint)
        
        lastLocation = location
        locationListener?.onLocationUpdate(location, speedKmh)
    }
    
    fun getRoutePoints(): List<RoutePoint> = routePoints.toList()
    
    fun getTotalDistance(): Double = totalDistance
    
    fun clearRoute() {
        routePoints.clear()
        totalDistance = 0.0
        lastLocation = null
    }
    
    fun getCurrentLocation(): Location? = _currentLocation.value
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun cleanup() {
        stopTracking()
        locationListener = null
    }
} 