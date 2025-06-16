package com.example.finalproject.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.finalproject.MainActivity
import com.example.finalproject.R
import com.example.finalproject.data.LiveRideStats
import com.example.finalproject.data.RideRepository
import com.example.finalproject.data.RideStatus
import com.example.finalproject.data.RoutePoint
import com.example.finalproject.utils.BleManager
import com.example.finalproject.utils.CrashDetection
import com.example.finalproject.utils.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class RideTrackingService : LifecycleService() {
    
    companion object {
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_PAUSE_TRACKING = "ACTION_PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "ACTION_RESUME_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ride_tracking_channel"
        
        fun startService(context: Context) {
            val intent = Intent(context, RideTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            context.startForegroundService(intent)
        }
        
        fun pauseService(context: Context) {
            val intent = Intent(context, RideTrackingService::class.java).apply {
                action = ACTION_PAUSE_TRACKING
            }
            context.startService(intent)
        }
        
        fun resumeService(context: Context) {
            val intent = Intent(context, RideTrackingService::class.java).apply {
                action = ACTION_RESUME_TRACKING
            }
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, RideTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
    
    private val binder = RideTrackingBinder()
    
    private lateinit var repository: RideRepository
    private lateinit var locationTracker: LocationTracker
    private lateinit var bleManager: BleManager
    private lateinit var crashDetection: CrashDetection
    
    private var currentRideId: Long? = null
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var totalPausedDuration: Long = 0
    
    private val _rideStats = MutableStateFlow(LiveRideStats())
    val rideStats: StateFlow<LiveRideStats> = _rideStats.asStateFlow()
    
    private var isTracking = false
    private var isPaused = false
    
    inner class RideTrackingBinder : Binder() {
        fun getService(): RideTrackingService = this@RideTrackingService
    }
    
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_PAUSE_TRACKING -> pauseTracking()
            ACTION_RESUME_TRACKING -> resumeTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        
        return START_STICKY
    }
    
    private fun initializeComponents() {
        repository = RideRepository(this)
        locationTracker = LocationTracker(this)
        bleManager = BleManager(this)
        crashDetection = CrashDetection(this)
        
        setupLocationTracking()
        setupBleTracking()
        setupCrashDetection()
    }
    
    private fun setupLocationTracking() {
        locationTracker.setLocationListener(object : LocationTracker.LocationListener {
            override fun onLocationUpdate(location: android.location.Location, speed: Double) {
                if (isTracking && !isPaused) {
                    val currentStats = _rideStats.value
                    val newPoint = RoutePoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        speed = speed,
                        altitude = location.altitude
                    )
                    
                    _rideStats.value = currentStats.copy(
                        currentSpeed = speed,
                        routePoints = currentStats.routePoints + newPoint,
                        maxSpeed = maxOf(currentStats.maxSpeed, speed)
                    )
                    updateNotification()
                }
            }
            
            override fun onDistanceUpdate(totalDistance: Double) {
                if (isTracking && !isPaused) {
                    _rideStats.value = _rideStats.value.copy(distance = totalDistance)
                }
            }
        })
    }
    
    private fun setupBleTracking() {
        bleManager.setBleListener(object : BleManager.BleListener {
            override fun onDeviceDiscovered(device: com.example.finalproject.data.BleDevice) {}
            override fun onDeviceConnected(device: com.example.finalproject.data.BleDevice) {}
            override fun onDeviceDisconnected(device: com.example.finalproject.data.BleDevice) {}
            override fun onConnectionError(device: com.example.finalproject.data.BleDevice, error: String) {}
            
            override fun onHeartRateUpdate(heartRate: Int) {
                if (isTracking) {
                    val currentStats = _rideStats.value
                    val newAverage = if (currentStats.averageHeartRate == 0) {
                        heartRate
                    } else {
                        (currentStats.averageHeartRate + heartRate) / 2
                    }
                    
                    _rideStats.value = currentStats.copy(
                        currentHeartRate = heartRate,
                        averageHeartRate = newAverage,
                        maxHeartRate = maxOf(currentStats.maxHeartRate, heartRate)
                    )
                }
            }
            
            override fun onPowerUpdate(power: Int) {
                // Can be used for advanced metrics
            }
        })
    }
    
    private fun setupCrashDetection() {
        crashDetection.setCrashListener(object : CrashDetection.CrashListener {
            override fun onCrashDetected(force: Float) {
                // Handle crash detection - could show emergency dialog
                // or automatically send location to emergency contacts
            }
            
            override fun onHighImpactDetected(force: Float) {
                // Handle high impact detection
            }
        })
    }
    
    private fun startTracking() {
        Log.d("RideTrackingService", "startTracking called")
        lifecycleScope.launch {
            try {
                currentRideId = repository.createNewRide()
                startTime = System.currentTimeMillis()
                isTracking = true
                isPaused = false
                totalPausedDuration = 0
                
                _rideStats.value = LiveRideStats(
                    isRecording = true,
                    isPaused = false,
                    startTime = startTime
                )
                
                locationTracker.startTracking()
                crashDetection.startMonitoring()
                
                startForeground(NOTIFICATION_ID, createNotification())
                startStatsUpdater()
                
            } catch (e: Exception) {
                Log.e("RideTrackingService", "Error in startTracking", e)
                stopSelf()
            }
        }
    }
    
    private fun pauseTracking() {
        if (isTracking && !isPaused) {
            isPaused = true
            pausedTime = System.currentTimeMillis()
            
            _rideStats.value = _rideStats.value.copy(isPaused = true)
            
            locationTracker.stopTracking()
            updateNotification()
            
            lifecycleScope.launch {
                currentRideId?.let { repository.pauseRide(it) }
            }
        }
    }
    
    private fun resumeTracking() {
        if (isTracking && isPaused) {
            isPaused = false
            totalPausedDuration += System.currentTimeMillis() - pausedTime
            
            _rideStats.value = _rideStats.value.copy(
                isPaused = false,
                pausedDuration = totalPausedDuration
            )
            
            locationTracker.startTracking()
            updateNotification()
            
            lifecycleScope.launch {
                currentRideId?.let { repository.resumeRide(it) }
            }
        }
    }
    
    private fun stopTracking() {
        isTracking = false
        isPaused = false
        
        locationTracker.stopTracking()
        crashDetection.stopMonitoring()
        
        lifecycleScope.launch {
            currentRideId?.let { rideId ->
                repository.finishRide(rideId, _rideStats.value)
            }
            
            _rideStats.value = LiveRideStats()
            currentRideId = null
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun startStatsUpdater() {
        Log.d("RideTrackingService", "startStatsUpdater called")
        lifecycleScope.launch {
            while (isTracking) {
                if (!isPaused) {
                    val currentTime = System.currentTimeMillis()
                    val duration = currentTime - startTime
                    val activeDuration = duration - totalPausedDuration
                    Log.d("RideTrackingService", "Updating stats: duration=$duration, activeDuration=$activeDuration")
                    
                    // Calculate calories (rough estimation)
                    val durationMinutes = activeDuration / 60000.0
                    val avgHeartRate = _rideStats.value.averageHeartRate.takeIf { it > 0 } ?: 120
                    val caloriesPerMinute = when {
                        avgHeartRate < 120 -> 8
                        avgHeartRate < 150 -> 12
                        else -> 16
                    }
                    val calories = (durationMinutes * caloriesPerMinute).toInt()
                    
                    _rideStats.value = _rideStats.value.copy(
                        duration = duration,
                        pausedDuration = totalPausedDuration,
                        calories = calories
                    )
                }
                
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing ride tracking status"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stats = _rideStats.value
        val distance = String.format("%.2f km", stats.distance)
        val duration = formatDuration(stats.activeDuration)
        val speed = String.format("%.1f km/h", stats.currentSpeed)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartBike - Recording")
            .setContentText("$distance • $duration • $speed")
            .setSmallIcon(R.drawable.ic_bike)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun formatDuration(duration: Long): String {
        val minutes = (duration / 60000).toInt()
        val seconds = ((duration % 60000) / 1000).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationTracker.cleanup()
        bleManager.cleanup()
        crashDetection.cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
} 