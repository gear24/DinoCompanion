package com.example.dinocompanionapp.data


object DinoProtocol {

    const val SCENE = "ESCENA"




// Android -> ESP32

    const val HELLO = "HELLO"

    const val BATTERY = "BATTERY"

    const val BRIGHTNESS = "BRILLO"



// ESP32 -> Android

    const val ACK = "ACK|"

    const val INFO = "INFO|"

    const val ERROR = "ERROR|"

    const val HELLO_RESPONSE = "HELLO|"

    const val BATTERY_RESPONSE = "BATTERY|"



}