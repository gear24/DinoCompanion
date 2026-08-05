package com.example.dinocompanionapp.data.audio

import android.content.Context
import android.media.AudioManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log


class VolumeManager(
    private val context: Context
){
    var onVolumeChanged: ((Int) -> Unit)? = null
    private var lastVolume = -1

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun getVolumePercent(): Int {

        val current =
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val max =
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return ((current.toFloat() / max) * 100).toInt()
    }

    fun start() {
        Log.d("DINO_VOLUME", "Iniciando VolumeManager")

        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {

                override fun onChange(selfChange: Boolean) {

                    val volume = getVolumePercent()

                    if (volume != lastVolume) {
                        lastVolume = volume

                        Log.d("DINO_VOLUME", "$volume%")
                        onVolumeChanged?.invoke(volume)
                    }
                }
            }
        )
    }
}