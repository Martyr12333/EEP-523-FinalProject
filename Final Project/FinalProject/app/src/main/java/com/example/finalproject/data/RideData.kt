package com.example.finalproject.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(tableName = "rides")
@TypeConverters(Converters::class)
@Parcelize
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val duration: Long, // in milliseconds
    val distance: Double, // in kilometers
    val averageSpeed: Double, // km/h
    val maxSpeed: Double, // km/h
    val calories: Int,
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val routePoints: List<RoutePoint>,
    val status: RideStatus,
    val pausedDuration: Long = 0 // time spent paused in milliseconds
) : Parcelable

@Parcelize
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speed: Double? = null,
    val altitude: Double? = null
) : Parcelable

enum class RideStatus {
    ACTIVE,      // Currently riding
    PAUSED,      // Paused but not finished
    COMPLETED,   // Finished and saved
    CANCELLED    // Cancelled without saving
}

data class LiveRideStats(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val currentSpeed: Double = 0.0,
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val pausedDuration: Long = 0L,
    val calories: Int = 0,
    val currentHeartRate: Int = 0,
    val averageHeartRate: Int = 0,
    val maxSpeed: Double = 0.0,
    val maxHeartRate: Int = 0,
    val routePoints: List<RoutePoint> = emptyList(),
    val startTime: Long = 0L
) {
    val activeDuration: Long
        get() = duration - pausedDuration
    
    val averageSpeed: Double
        get() = if (activeDuration > 0) {
            distance / (activeDuration / 1000.0 / 3600.0)
        } else 0.0
}

@Parcelize
data class BleDevice(
    val name: String?,
    val address: String,
    val deviceType: DeviceType,
    val isConnected: Boolean = false,
    val rssi: Int = 0
) : Parcelable

enum class DeviceType {
    HEART_RATE_MONITOR,
    CYCLING_SPEED_SENSOR,
    CYCLING_CADENCE_SENSOR,
    CYCLING_POWER_SENSOR,
    UNKNOWN
}

// Type converters for Room database
class Converters {
    @TypeConverter
    fun fromRoutePointList(value: List<RoutePoint>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toRoutePointList(value: String): List<RoutePoint> {
        return try {
            Gson().fromJson(value, object : TypeToken<List<RoutePoint>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromRideStatus(status: RideStatus): String {
        return status.name
    }

    @TypeConverter
    fun toRideStatus(status: String): RideStatus {
        return try {
            RideStatus.valueOf(status)
        } catch (e: Exception) {
            RideStatus.CANCELLED
        }
    }
} 