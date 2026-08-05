package com.example.dinocompanionapp.data.audio


data class MediaState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
    val position: Long = 0L,
    val isPlaying: Boolean = false
)