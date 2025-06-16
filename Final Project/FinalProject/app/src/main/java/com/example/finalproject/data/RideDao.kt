package com.example.finalproject.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    
    @Query("SELECT * FROM rides ORDER BY startTime DESC")
    fun getAllRides(): Flow<List<RideEntity>>
    
    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: Long): RideEntity?
    
    @Query("SELECT * FROM rides WHERE status = :status ORDER BY startTime DESC")
    fun getRidesByStatus(status: RideStatus): Flow<List<RideEntity>>
    
    @Query("SELECT * FROM rides WHERE status = 'COMPLETED' ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCompletedRides(limit: Int): Flow<List<RideEntity>>
    
    @Query("SELECT COUNT(*) FROM rides WHERE status = 'COMPLETED'")
    fun getCompletedRidesCount(): Flow<Int>
    
    @Query("SELECT SUM(distance) FROM rides WHERE status = 'COMPLETED'")
    fun getTotalDistance(): Flow<Double?>
    
    @Query("SELECT SUM(duration - pausedDuration) FROM rides WHERE status = 'COMPLETED'")
    fun getTotalActiveTime(): Flow<Long?>
    
    @Query("SELECT AVG(averageSpeed) FROM rides WHERE status = 'COMPLETED' AND averageSpeed > 0")
    fun getOverallAverageSpeed(): Flow<Double?>
    
    @Query("SELECT SUM(calories) FROM rides WHERE status = 'COMPLETED'")
    fun getTotalCalories(): Flow<Int?>
    
    @Query("SELECT * FROM rides WHERE status IN ('ACTIVE', 'PAUSED') LIMIT 1")
    suspend fun getActiveRide(): RideEntity?
    
    @Insert
    suspend fun insertRide(ride: RideEntity): Long
    
    @Update
    suspend fun updateRide(ride: RideEntity)
    
    @Delete
    suspend fun deleteRide(ride: RideEntity)
    
    @Query("DELETE FROM rides WHERE id = :rideId")
    suspend fun deleteRideById(rideId: Long)
    
    @Query("DELETE FROM rides WHERE status = 'CANCELLED'")
    suspend fun deleteCancelledRides()
    
    @Query("UPDATE rides SET status = 'CANCELLED' WHERE status IN ('ACTIVE', 'PAUSED')")
    suspend fun cancelActiveRides()
} 