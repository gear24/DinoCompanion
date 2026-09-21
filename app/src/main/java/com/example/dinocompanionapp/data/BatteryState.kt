package com.example.dinocompanionapp.data

data class BatteryState(
    val porcentaje: Int = -1,
    val estado: String = "Normal",
    val raw: Int? = null,
    val voltaje: Float? = null,
    val disponible: Boolean = false

)