package com.example.dinocompanionapp.data.audio


data class AudioAnalysis(
    val energy: Int = 0,
    val bass: Int = 0,
    val mids: Int = 0,
    val treble: Int = 0,
    val beat: Boolean = false,
    val bpm: Int = 0
)