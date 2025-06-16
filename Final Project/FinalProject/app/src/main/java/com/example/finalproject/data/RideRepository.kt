package com.example.finalproject.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RideRepository(context: Context) {
    
    private val rideDao = RideDatabase.getDatabase(context).rideDao()
    
    fun getAllRides(): Flow<List<RideEntity>> = rideDao.getAllRides()
    
    fun getCompletedRides(): Flow<List<RideEntity>> = 
        rideDao.getRidesByStatus(RideStatus.COMPLETED)
    
    fun getRecentRides(limit: Int = 10): Flow<List<RideEntity>> = 
        rideDao.getRecentCompletedRides(limit)
    
    suspend fun getRideById(id: Long): RideEntity? = rideDao.getRideById(id)
    
    suspend fun getActiveRide(): RideEntity? = rideDao.getActiveRide()
    
    suspend fun insertRide(ride: RideEntity): Long = rideDao.insertRide(ride)
    
    suspend fun updateRide(ride: RideEntity) = rideDao.updateRide(ride)
    
    suspend fun deleteRide(ride: RideEntity) = rideDao.deleteRide(ride)
    
    suspend fun deleteRideById(id: Long) = rideDao.deleteRideById(id)
    
    suspend fun cancelActiveRides() = rideDao.cancelActiveRides()
    
    suspend fun deleteCancelledRides() = rideDao.deleteCancelledRides()
    
    // Statistics
    fun getTotalDistance(): Flow<Double> = 
        rideDao.getTotalDistance().map { it ?: 0.0 }
    
    fun getTotalActiveTime(): Flow<Long> = 
        rideDao.getTotalActiveTime().map { it ?: 0L }
    
    fun getAverageSpeed(): Flow<Double> = 
        rideDao.getOverallAverageSpeed().map { it ?: 0.0 }
    
    fun getTotalCalories(): Flow<Int> = 
        rideDao.getTotalCalories().map { it ?: 0 }
    
    fun getCompletedRidesCount(): Flow<Int> = rideDao.getCompletedRidesCount()
    
    // Helper methods
    suspend fun createNewRide(): Long {
        val newRide = RideEntity(
            startTime = System.currentTimeMillis(),
            endTime = null,
            duration = 0L,
            distance = 0.0,
            averageSpeed = 0.0,
            maxSpeed = 0.0,
            calories = 0,
            averageHeartRate = 0,
            maxHeartRate = 0,
            routePoints = emptyList(),
            status = RideStatus.ACTIVE,
            pausedDuration = 0L
        )
        return insertRide(newRide)
    }
    
    suspend fun finishRide(rideId: Long, finalStats: LiveRideStats): Boolean {
        val ride = getRideById(rideId) ?: return false
        
        val finishedRide = ride.copy(
            endTime = System.currentTimeMillis(),
            duration = finalStats.duration,
            distance = finalStats.distance,
            averageSpeed = finalStats.averageSpeed,
            maxSpeed = finalStats.maxSpeed,
            calories = finalStats.calories,
            averageHeartRate = finalStats.averageHeartRate,
            maxHeartRate = finalStats.maxHeartRate,
            routePoints = finalStats.routePoints,
            status = RideStatus.COMPLETED,
            pausedDuration = finalStats.pausedDuration
        )
        
        updateRide(finishedRide)
        return true
    }
    
    suspend fun pauseRide(rideId: Long): Boolean {
        val ride = getRideById(rideId) ?: return false
        if (ride.status == RideStatus.ACTIVE) {
            updateRide(ride.copy(status = RideStatus.PAUSED))
            return true
        }
        return false
    }
    
    suspend fun resumeRide(rideId: Long): Boolean {
        val ride = getRideById(rideId) ?: return false
        if (ride.status == RideStatus.PAUSED) {
            updateRide(ride.copy(status = RideStatus.ACTIVE))
            return true
        }
        return false
    }
    
    suspend fun cancelRide(rideId: Long): Boolean {
        val ride = getRideById(rideId) ?: return false
        if (ride.status in listOf(RideStatus.ACTIVE, RideStatus.PAUSED)) {
            updateRide(ride.copy(status = RideStatus.CANCELLED))
            return true
        }
        return false
    }
} 