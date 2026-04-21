package com.hk.vcab.models

data class Passenger(
    val uid: String,
    val name: String,
    val regNo: String,
    val phone: String,
    val email: String,
    val gender: String,
    val passengerCount: Int,
    val isMe: Boolean
)
