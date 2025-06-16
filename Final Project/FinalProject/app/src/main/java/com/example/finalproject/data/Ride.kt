package com.example.finalproject.data

data class Ride(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long,
    val pausedDuration: Long = 0,
    val distance: Double,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val calories: Int,
    val averageHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val routePoints: List<RoutePoint> = emptyList(),
    val status: RideStatus = RideStatus.COMPLETED
) 