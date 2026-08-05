package com.example.dinocompanionapp.data


object DinoProtocol {

    const val SCENE = "ESCENA"




// Android -> ESP32

    const val HELLO = "HELLO"
    const val BATTERY = "BATTERY"
    const val BRIGHTNESS = "BRILLO"
    const val SET_NAME = "NAME|"
    const val RESTART = "RESTART"


// ESP32 -> Android

    const val ACK = "ACK|"

    const val INFO = "INFO|"

    const val ERROR = "ERROR|"

    const val HELLO_RESPONSE = "HELLO|"

    const val BATTERY_RESPONSE = "BATTERY|"

    // Media Management

    const val DEVICE = "DEVICE"
    const val MEDIA = "MEDIA"
    const val DSP = "DSP"
    const val TEMP = "TEMP"


    // --- AUDIO / MÚSICA ---
    const val MUSIC = "MUSIC"
    const val MUSIC_PLAY = "PLAY"
    const val MUSIC_PAUSE = "PAUSE"
    const val MUSIC_SONG = "SONG|"
    const val VOLUME = "VOLUMEN|"


}