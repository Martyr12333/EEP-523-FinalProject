package com.example.finalproject.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class CrashDetection(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Crash detection parameters
    private val crashThreshold = 25.0f // G-force threshold for crash detection
    private val debounceTime = 3000L // 3 seconds debounce
    private var lastCrashTime = 0L
    
    // State tracking
    private val _crashDetected = MutableStateFlow(false)
    val crashDetected: StateFlow<Boolean> = _crashDetected
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring
    
    private var crashListener: CrashListener? = null
    
    interface CrashListener {
        fun onCrashDetected(force: Float)
        fun onHighImpactDetected(force: Float)
    }
    
    fun startMonitoring() {
        if (accelerometer != null && !_isMonitoring.value) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
            _isMonitoring.value = true
        }
    }
    
    fun stopMonitoring() {
        if (_isMonitoring.value) {
            sensorManager.unregisterListener(this)
            _isMonitoring.value = false
        }
    }
    
    fun setCrashListener(listener: CrashListener?) {
        this.crashListener = listener
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = sensorEvent.values[0]
                val y = sensorEvent.values[1]
                val z = sensorEvent.values[2]
                
                // Calculate total acceleration force
                val force = sqrt(x * x + y * y + z * z)
                
                // Remove gravity (approximately 9.8 m/s²)
                val netForce = force - SensorManager.GRAVITY_EARTH
                
                // Check for crash threshold
                if (netForce > crashThreshold) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Debounce to prevent multiple detections
                    if (currentTime - lastCrashTime > debounceTime) {
                        lastCrashTime = currentTime
                        _crashDetected.value = true
                        crashListener?.onCrashDetected(netForce)
                        
                        // Reset crash detected state after a delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            _crashDetected.value = false
                        }, debounceTime)
                    }
                } else if (netForce > crashThreshold * 0.6f) {
                    // High impact but not crash-level
                    crashListener?.onHighImpactDetected(netForce)
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }
    
    fun cleanup() {
        stopMonitoring()
        crashListener = null
    }
} 