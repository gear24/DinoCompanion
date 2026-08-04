package com.example.dinocompanionapp.data

data class DinoInfo(
    val name: String = "Dino",
    val firmware: String = "",
    val hardware: String = "",
    val serial: String = "",
    val battery: Int = -1,
    val temperature: Float = 0f
)