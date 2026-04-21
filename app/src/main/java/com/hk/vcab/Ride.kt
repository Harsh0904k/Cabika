package com.hk.vcab.models

data class Ride(
    val id: String = "",
    val pickup: String = "",
    val drop: String = "",
    val date: String = "",
    val time: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorPassengerCount: Int = 1,
    val joinedUids: Map<String, Int> = emptyMap() // uid -> passenger count
)
