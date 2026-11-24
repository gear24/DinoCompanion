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

    // --- FIESTA ---
    const val FIESTASTYLE = "FIESTASTYLE|"
    const val AUTOFIESTA  = "AUTOFIESTA|"

    // --- PANZA (¡ojo: respuesta comparte prefijo con comandos!) ---
    const val PANZA_ON       = "PANZA|ON"
    const val PANZA_OFF      = "PANZA|OFF"
    const val PANZA_TOGGLE   = "PANZA|TOGGLE"   // por si el firmware lo usa
    const val PANZA_RESPONSE = "PANZA|"         // broadcast de estado PANZA|0 / PANZA|1

}