package com.example.dinocompanionapp.data.audio

import kotlin.toString
import android.content.Context
import com.example.dinocompanionapp.data.audio.MediaState
import com.example.dinocompanionapp.services.MediaListenerService
import android.provider.Settings
import android.content.Intent
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.util.Log
import android.media.session.MediaController



// --- METADATA DE MÚSICA ---
// Obtiene la información multimedia activa del teléfono (título, artista, etc.).

class MediaSessionManager(
    private val context: Context
) {
    // --- COMUNICACIÓN CON VIEWMODEL ---
// Envía cambios de música al resto de la app.
    var onMediaChanged: ((MediaState) -> Unit)? = null

    // --- CALLBACK DE CAMBIOS MULTIMEDIA ---
// Escucha cambios de canción, play/pause, etc.

    private val controllerCallback =
        object : MediaController.Callback() {

            override fun onMetadataChanged(
                metadata: MediaMetadata?
            ) {
                updateMediaSession()
            }


            override fun onPlaybackStateChanged(
                state: PlaybackState?
            ) {
                updateMediaSession()

                onPlaybackChanged?.invoke(
                    state?.state == PlaybackState.STATE_PLAYING
                )
            }
        }
    var onPlaybackChanged: ((Boolean)->Unit)? = null

    private var currentController: MediaController? = null
    private var lastMediaState: MediaState? = null

    // --- CALLBACKS ---
// --- CONTROL DEL SERVICIO ---

    fun start() {
        Log.d("DINO_AUDIO_DEBUG", "MediaSessionManager.start()")

        MediaListenerService.onSessionChanged = {
            Log.d("DINO_AUDIO_DEBUG", "onSessionChanged")
            actualizarSesion()
        }

        if (hasNotificationAccess()) {
            actualizarSesion()
        }
    }


    // --- SESIÓN MULTIMEDIA ---
    private fun actualizarSesion() {
        updateMediaSession()
    }

    // --- PERMISOS ---
// Comprueba si la app tiene acceso a las sesiones multimedia.

    fun hasNotificationAccess(): Boolean {

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(context.packageName)
    }

    // --- PERMISOS ---
// Abre la pantalla donde el usuario concede acceso a las sesiones multimedia.

    fun requestNotificationAccess() {

        val intent = Intent(
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    // --- SESIONES MULTIMEDIA ---
// Obtiene el administrador de sesiones multimedia de Android.

    private val mediaSessionManager by lazy {

        context.getSystemService(
            Context.MEDIA_SESSION_SERVICE
        ) as android.media.session.MediaSessionManager

    }

// --- SESIONES MULTIMEDIA ---
// Busca la sesión multimedia con mayor prioridad.

    fun updateMediaSession() {

        val component = ComponentName(
            context,
            MediaListenerService::class.java
        )

        val sessions = mediaSessionManager
            .getActiveSessions(component)

//        Log.d(
//            "DINO_AUDIO_Media_Session",
//            "Sesiones activas: ${sessions.size}"
//        )

        val session = sessions.firstOrNull() ?: return

        if (currentController != session) {

            currentController?.unregisterCallback(controllerCallback)

            currentController = session

            currentController?.registerCallback(controllerCallback)
        }

        val metadata = session.metadata
        val playback = session.playbackState

        val media = MediaState(

            title = metadata?.getString(
                MediaMetadata.METADATA_KEY_TITLE
            ) ?: "",

            artist = metadata?.getString(
                MediaMetadata.METADATA_KEY_ARTIST
            ) ?: "",

            album = metadata?.getString(
                MediaMetadata.METADATA_KEY_ALBUM
            ) ?: "",

            duration = metadata?.getLong(
                MediaMetadata.METADATA_KEY_DURATION
            ) ?: 0L,

            position = playback?.position ?: 0L,

            isPlaying = playback?.state ==
                    PlaybackState.STATE_PLAYING
        )


        if (media != lastMediaState) {

            lastMediaState = media



            onMediaChanged?.invoke(media)
        }
        onMediaChanged?.invoke(media)
    }
}



