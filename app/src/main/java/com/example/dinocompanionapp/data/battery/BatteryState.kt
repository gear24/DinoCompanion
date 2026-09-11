package com.example.dinocompanionapp.data.battery

data class BatteryState(
    val porcentaje: Int = -1,
    val estado: String = "Normal",
    val raw: Int? = null,
    val voltaje: Float? = null
)