package com.example.dinocompanionapp.bluetooth

import android.util.Log
import com.example.dinocompanionapp.data.MediaState




// --- ESTADO DE MÚSICA ---
// Guarda la última información recibida del teléfono.



class MusicManager {

    private var lastMedia: MediaState? = null


    // --- EVENTOS DE MÚSICA ---
    // Detecta cambios importantes de reproducción.

    var onSongChanged: ((MediaState) -> Unit)? = null

    var onPlaybackChanged: ((Boolean) -> Unit)? = null


    fun update(media: MediaState) {

        val oldMedia = lastMedia


        if (oldMedia?.title != media.title ||
            oldMedia?.artist != media.artist
        ) {

            Log.d(
                "DINO_AUDIO_MusicManager",
                "Canción nueva: ${media.title}"
            )

            onSongChanged?.invoke(media)
        }


        if (oldMedia?.isPlaying != media.isPlaying) {

            Log.d(
                "DINO_AUDIO_MusicManager_2",
                "Estado reproducción: ${media.isPlaying}"
            )

            onPlaybackChanged?.invoke(media.isPlaying)
        }


        lastMedia = media
    }
}