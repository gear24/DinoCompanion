package com.example.dinocompanionapp.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification


// --- SERVICIO DE METADATA ---
// Escucha cambios en las sesiones multimedia del sistema.

class MediaListenerService : NotificationListenerService() {

    // --- CALLBACKS ---

    companion object {

        var onSessionChanged: (() -> Unit)? = null

    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        onSessionChanged?.invoke()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification?
    ) {
        super.onNotificationPosted(sbn)

        onSessionChanged?.invoke()
    }
}