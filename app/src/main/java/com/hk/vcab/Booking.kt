package com.hk.vcab

data class Booking(
    var rideId: String = "",
    var name: String = "",
    var pickup: String = "",
    var drop: String = "",
    var date: String = "",
    var time: String = "",
    var passengerCount: Long = 0,
    var carType: String = "",
    var uid: String = "",
    var driverId: String? = null,
    var driverName: String? = null,
    var driverEmail: String? = null,
    var driverPhone: String? = null,
    var isCabika: Boolean = false,
    var visibility: String = "no",
    var driverRequired: Boolean = true   // ✅ NEW FIELD — true = needs cab driver, false = only travel partner
)
