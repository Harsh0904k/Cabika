package com.hk.vcab.models

data class OngoingRide(
    val rideId: String = "",
    val pickup: String = "",
    val drop: String = "",
    val date: String = "",
    val time: String = "",
    val joinedUsers: Int = 0,
    val maxSeats: Int,
    val creatorPassengerCount: Int,
    val driverAssigned: Boolean = false
)
