package com.example.dinocompanionapp.bluetooth

import com.example.dinocompanionapp.data.*

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class BluetoothManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            appContext.getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBluetoothManager

        bluetoothManager.adapter
    }

    private var socket: BluetoothSocket? = null
    var onMessageReceived: ((String) -> Unit)? = null

    private var device: BluetoothDevice? = null
    var onConnectionLost: (() -> Unit)? = null

    private companion object {
        const val DEVICE_NAME = "Dino_Ritmico_Prototipo"

        val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var listenJob: Job? = null
    private var shouldReconnect = false

    var state by mutableStateOf(BtState.DISCONNECTED)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set


    val statusText: String
        get() = when (state) {
            BtState.DISCONNECTED -> "🔴 Desconectado"
            BtState.CONNECTING -> "🟡 Conectando..."
            BtState.CONNECTED -> "🟢 Conectado"
            BtState.RECONNECTING -> "🟠 Reconectando..."
            BtState.ERROR -> "❌ Error"
        }


    init {
        checkBluetoothStatus()
    }


    @SuppressLint("MissingPermission")
    fun checkBluetoothStatus() {

        if (bluetoothAdapter?.isEnabled != true) {

            lastError = "Bluetooth apagado. Actívalo para conectar con Dino."
            state = BtState.ERROR

        } else if (
            state == BtState.ERROR &&
            lastError?.contains("Bluetooth apagado") == true
        ) {

            lastError = null
            state = BtState.DISCONNECTED
        }
    }


    private fun log(msg: String) {
        Log.d("BluetoothManager", msg)
    }


    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }


    private fun clearConnection() {

        try {
            listenJob?.cancel()
            socket?.close()

        } catch (_: Exception) {}

        socket = null
        device = null
    }


    fun hasBtPermission(): Boolean {

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            true
        }
    }


    @SuppressLint("MissingPermission")
    suspend fun connect(enableReconnect: Boolean = true) {

        lastError = null

        if (enableReconnect) {
            shouldReconnect = false
        }


        if (state == BtState.CONNECTING) return
        if (isConnected()) return


        withContext(Dispatchers.Main) {

            if (state != BtState.RECONNECTING) {
                state = BtState.CONNECTING
            }
        }


        if (!hasBtPermission()) {

            withContext(Dispatchers.Main) {
                lastError = "Bluetooth sin permisos."
                state = BtState.ERROR
            }

            log("Sin permisos Bluetooth")
            return
        }


        if (bluetoothAdapter?.isEnabled != true) {

            withContext(Dispatchers.Main) {

                lastError =
                    "Bluetooth apagado. Actívalo para conectar con Dino."

                state = BtState.ERROR
            }

            return
        }


        withContext(Dispatchers.IO) {

            try {

                log("Buscando Dino...")


                device = bluetoothAdapter
                    ?.bondedDevices
                    ?.firstOrNull {
                        it.name == DEVICE_NAME
                    }


                if (device == null) {

                    withContext(Dispatchers.Main) {

                        lastError =
                            "No pudimos conectar con tu Dino. Verifica que esté encendido y dentro del alcance Bluetooth."

                        state = BtState.ERROR
                    }

                    return@withContext
                }


                bluetoothAdapter?.cancelDiscovery()


                val newSocket =
                    requireNotNull(device)
                        .createRfcommSocketToServiceRecord(SPP_UUID)


                newSocket.connect()

                socket = newSocket

                shouldReconnect = true


                withContext(Dispatchers.Main) {

                    state = BtState.CONNECTED
                    lastError = null
                }


                startListening()
                send("HELLO")


                log("Conectado correctamente")


            } catch (e: Exception) {

                log("Error: ${e.message}")

                clearConnection()

                withContext(Dispatchers.Main) {

                    lastError =
                        "❌ No pudimos conectar con tu Dino.\nVerifica que esté encendido y dentro del alcance Bluetooth."

                    state = BtState.ERROR
                }
            }
        }
    }


    fun disconnect() {

        shouldReconnect = false

        log("Desconectando...")

        listenJob?.cancel()

        clearConnection()

        state = BtState.DISCONNECTED
        lastError = null
    }


    suspend fun send(message: String): Boolean =
        withContext(Dispatchers.IO) {

            if (!isConnected()) {

                log("No hay conexión activa")

                withContext(Dispatchers.Main) {
                    state = BtState.DISCONNECTED
                }

                return@withContext false
            }


            try {

                withTimeout(600.milliseconds) {

                    val stream =
                        socket?.outputStream
                            ?: throw IOException("Socket no disponible")


                    stream.write(
                        (message + "\n").toByteArray()
                    )

                    stream.flush()

                    true
                }


            } catch (_: TimeoutCancellationException) {

                log("Timeout enviando mensaje")

                clearConnection()

                withContext(Dispatchers.Main) {

                    lastError = "La conexión no responde."
                    state = BtState.ERROR
                }

                false


            } catch (e: IOException) {

                log("Error de E/S: ${e.message}")

                clearConnection()

                withContext(Dispatchers.Main) {

                    lastError = "Conexión interrumpida."
                    state = BtState.ERROR
                }

                false


            } catch (e: Exception) {

                log("Error imprevisto: ${e.message}")

                clearConnection()

                withContext(Dispatchers.Main) {

                    lastError = "Conexión perdida."
                    state = BtState.ERROR
                }

                false
            }
        }


    @SuppressLint("MissingPermission")
    private fun startListening() {

        listenJob?.cancel()

        listenJob = scope.launch {

            try {

                val buffer = ByteArray(1024)

                while (isConnected()) {

                    val bytes =
                        socket?.inputStream?.read(buffer)
                            ?: break


                    if (bytes > 0) {

                        val message =
                            String(buffer, 0, bytes)


                        log("Recibido: $message")


                        withContext(Dispatchers.Main) {

                            onMessageReceived
                                ?.invoke(message.trim())
                        }
                    }
                }


            } catch (_: IOException) {

                log("Conexión perdida")

                clearConnection()


                if (shouldReconnect) {

                    startReconnectLoop()

                } else {

                    withContext(Dispatchers.Main) {
                        state = BtState.DISCONNECTED
                    }
                }


                withContext(Dispatchers.Main) {
                    onConnectionLost?.invoke()
                }


            } catch (e: Exception) {

                log("Error escuchando: ${e.message}")

                clearConnection()

                withContext(Dispatchers.Main) {

                    lastError =
                        "Ocurrió un error en la comunicación."

                    state = BtState.ERROR
                }
            }
        }
    }


    private fun startReconnectLoop() {

        scope.launch {

            while (shouldReconnect && !isConnected()) {

                withContext(Dispatchers.Main) {
                    state = BtState.RECONNECTING
                }


                delay(3.seconds)
                connect(false)
            }
        }
    }
}