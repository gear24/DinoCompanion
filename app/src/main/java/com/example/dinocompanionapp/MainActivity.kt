package com.example.dinocompanionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinocompanionapp.ui.theme.DinoCompanionAppTheme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults
import android.bluetooth.BluetoothAdapter
import java.util.UUID
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import com.example.dinocompanionapp.DinoProtocol
import kotlinx.coroutines.launch

val Cream = Color(0xFFFFF0F0)
val Pink = Color(0xFFF75590)
val SoftPink = Color(0xFFFCE4D8)
val Dark = Color(0xFF5B2333)
val Beige = Color(0xFFF7E7CE)
val Sand = Color(0xFFC2B280)
val Brown = Color(0xFF5D4037)

val SPP_UUID: UUID = UUID.fromString(
    "00001101-0000-1000-8000-00805F9B34FB"
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DinoCompanionAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DinoHome(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


    @Composable
    fun ColorPicker(
        currentColor: Color,
        onColorChanged: (Color) -> Unit,
        sendColor: (Int, Int, Int) -> Unit
    ) {
        var touchX by remember { mutableStateOf(0f) }
        var touchY by remember { mutableStateOf(0f) }

        var drawX by remember { mutableStateOf(0f) }
        var drawY by remember { mutableStateOf(0f) }
        val ease = 0.15f

        var lastSendTime by remember { mutableStateOf(0L) }
        var lastColor by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {

                drawX += (touchX - drawX) * ease
                drawY += (touchY - drawY) * ease

                delay(16)
            }
        }

        var cursorInitialized by remember {
            mutableStateOf(false)
        }


        LaunchedEffect(currentColor) {

            val hsv = FloatArray(3)

            android.graphics.Color.colorToHSV(
                currentColor.toArgb(),
                hsv
            )

            touchX = hsv[0] / 360f
            touchY = 1f - hsv[2]

            cursorInitialized = true
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(18.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())


                        val width = size.width.toFloat()
                        val height = size.height.toFloat()

                        touchX = x / width
                        touchY = y / height


                        val hue = touchX * 360f
                        val value = 1f - touchY

                        val color = Color.hsv(
                            hue,
                            1f,
                            value.coerceIn(0.2f, 1f)
                        )

                        val now = System.currentTimeMillis()

                        if (color.toArgb() != lastColor && now - lastSendTime > 40) {

                            lastColor = color.toArgb()
                            lastSendTime = now

                            onColorChanged(color)

                            sendColor(
                                (color.red * 255).toInt(),
                                (color.green * 255).toInt(),
                                (color.blue * 255).toInt()
                            )
                        }
                    }
                }

        ) {

            // 🎨 Fondo gradiente tipo "Photoshop"
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            )

            // 💀 overlay de negro hacia abajo (para value/brightness)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black
                    )
                )
            )

            // 🎯 cursor
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 16f,
                center = Offset(
                    drawX * size.width + 2,
                    drawY * size.height + 2
                )
            )

            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(
                    drawX * size.width,
                    drawY * size.height
                )
            )

            drawCircle(
                color = Color.Black,
                radius = 10f,
                style = Stroke(2f),
                center = Offset(
                    drawX * size.width,
                    drawY * size.height
                )
            )
        }
    }


    @Composable
    fun DinoHome(modifier: Modifier = Modifier) {

        var mensaje by remember { mutableStateOf("") }
        var pantalla by remember { mutableStateOf("home") }
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        val context = androidx.compose.ui.platform.LocalContext.current


        val bluetoothManager = remember {
            BluetoothManager(context)
        }
        var ultimoMensaje by remember {
            mutableStateOf("")
        }
        var bateria by remember {
            mutableIntStateOf(-1)
        }
        var infoDino by remember {
            mutableStateOf("")
        }
        var lastError by remember {
            mutableStateOf("")
        }
        val scope = rememberCoroutineScope()


        fun procesarMensaje(message: String) {

            when {

                message.startsWith(DinoProtocol.ACK) -> {

                    Log.d("ESP32", message)

                }

                message.startsWith(DinoProtocol.HELLO_RESPONSE) -> {

                    Log.d("ESP32", "Firmware iniciado")

                    bluetoothManager.send(DinoProtocol.BATTERY)

                }

                message.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {

                    val porcentaje = message
                        .substringAfter("|")
                        .toIntOrNull()

                    if (porcentaje != null) {
                        bateria = porcentaje
                    }

                }

                message.startsWith(DinoProtocol.INFO) -> {

                    Log.d("ESP32", message)

                }

                message.startsWith(DinoProtocol.ERROR) -> {

                    Log.e("ESP32", message)

                }

                else -> {

                    Log.d("ESP32", message)

                }

            }

        }

        LaunchedEffect(bluetoothManager) {

            bluetoothManager.onMessageReceived = { message ->

                procesarMensaje(message)

            }

        }
        LaunchedEffect(Unit) {

            scope.launch {

                bluetoothManager.connect()

            }

        }
        val textoConexion = when (bluetoothManager.state) {

            BtState.DISCONNECTED ->
                "🔴 Conectar Dino"

            BtState.CONNECTING ->
                "🟡 Conectando..."

            BtState.CONNECTED ->
                "🟢 Desconectar Dino"

            BtState.RECONNECTING ->
                "🟠 Reconectando..."

            BtState.ERROR ->
                "🔴 Conectar Dino"
        }

        val activity = context as ComponentActivity
        val prefs = remember {
            context.getSharedPreferences(
                "dino_settings",
                Context.MODE_PRIVATE
            )
        }

        val enableBtLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {

                scope.launch {

                    bluetoothManager.connect()

                }

            }

        }

        var brillo by remember {
            mutableStateOf(prefs.getFloat("brillo", 80f))
        }
        var modoActual by remember {

            mutableStateOf(
                prefs.getInt(
                    "modo_actual",
                    0
                )
            )

        }

        var dinoEncendido by remember {

            mutableStateOf(
                prefs.getBoolean(
                    "dino_encendido",
                    false
                )
            )

        }

        fun sendBT(message: String): Boolean {

            return bluetoothManager.send(message)

        }

        fun sendMode(mode: Int) {
            bluetoothManager.send(mode.toString())
        }

        fun sendColor(
            r: Int,
            g: Int,
            b: Int
        ) {
            bluetoothManager.send("$r,$g,$b")
        }


        fun sendBrightness(value: Int) {
            bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|$value")
        }

        fun aplicarEscena(escena: Escena) {
            val velocidad = 6
            val cantColores = escena.colores.size

            // Usamos directamente escena.efecto.codigo
            val stringEscena = StringBuilder(
                "${DinoProtocol.SCENE}|${escena.efecto.codigo}|$velocidad|$cantColores"
            )
            escena.colores.forEach { colorArgb ->
                val color = Color(colorArgb)
                val r = (color.red * 255).toInt()
                val g = (color.green * 255).toInt()
                val b = (color.blue * 255).toInt()
                stringEscena.append("|$r|$g|$b")
            }

            sendBrightness(escena.brillo)
            sendBT(stringEscena.toString())
        }


        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Pink)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Dino Companion")

            Spacer(Modifier.height(24.dp))

            DinoButton(textoConexion) {

                when (bluetoothManager.state) {

                    BtState.DISCONNECTED,
                    BtState.ERROR -> {

                        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) {

                            enableBtLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )

                        } else {

                            scope.launch {
                                bluetoothManager.connect()
                            }

                        }

                    }

                    BtState.CONNECTED -> {

                        bluetoothManager.disconnect()

                    }

                    else -> {}

                }

            }

            Spacer(Modifier.height(8.dp))

            Text(bluetoothManager.statusText)
            //borrar luego
            if (ultimoMensaje.isNotBlank()) {

                Text("📨 $ultimoMensaje")
                Text("papas")

            }

            bluetoothManager.lastError?.let {

                Spacer(Modifier.height(4.dp))

                Text(it)

            }
            if (bateria >= 0) {

                Text("🔋 $bateria%")

            }
            // esto se usara para hacer snacks que son como mensajes pop up de 2 segunds solo para cuando
            // ERROR|
            //LOW_BATTERY|
            //CHARGING|
            //FULL|
            //UPDATE|
            /*
        if (infoDino.isNotBlank()) {

            Text("🦖 $infoDino")

        } */

            DinoButton("Colores Favoritos") {
                pantalla = "colors"

            }

            DinoButton("💡 Modos de Luz") {
                pantalla = "modes"
            }
            DinoButton("Creador de Escenas") {
                pantalla = "scenes"
            }

            DinoButton("⛔ Apagar") {

                dinoEncendido = false

                prefs.edit()
                    .putBoolean(
                        "dino_encendido",
                        false
                    )
                    .apply()

                sendMode(0)
            }

            Spacer(Modifier.height(24.dp))

            Text(mensaje)

            Spacer(Modifier.height(24.dp))

            when (pantalla) {

                "home" -> DinoCard("🏠 Home del Dino")

                "music" -> DinoCard("🎵 Música")


                "colors" -> Column {

                    DinoCard("Colores Favoritos")

                    Spacer(Modifier.height(20.dp))


                    var hue by remember { mutableStateOf(0f) }


                    val prefs = remember {
                        context.getSharedPreferences(
                            "dino_settings",
                            Context.MODE_PRIVATE
                        )
                    }

                    var favoritos by remember {

                        mutableStateOf(
                            List<Color?>(5) { index ->

                                if (prefs.contains("favorito_$index")) {

                                    Color(
                                        prefs.getInt(
                                            "favorito_$index",
                                            Color.Transparent.toArgb()
                                        )
                                    )

                                } else {
                                    null
                                }

                            }
                        )

                    }


                    var currentColor by remember {

                        mutableStateOf(

                            Color(
                                prefs.getInt(
                                    "current_color",
                                    Color.Red.toArgb()
                                )
                            )

                        )

                    }

                    val hsv = FloatArray(3)

                    LaunchedEffect(Unit) {

                        val colorGuardado = Color(
                            prefs.getInt(
                                "current_color",
                                Color.Red.toArgb()
                            )
                        )

                        val brilloGuardado = prefs.getFloat(
                            "brillo",
                            80f
                        )
                        currentColor = colorGuardado

                        brillo = brilloGuardado


                        sendColor(
                            (colorGuardado.red * 255).toInt(),
                            (colorGuardado.green * 255).toInt(),
                            (colorGuardado.blue * 255).toInt()
                        )


                        sendBrightness(
                            brilloGuardado.toInt()
                        )

                    }
                    android.graphics.Color.colorToHSV(
                        currentColor.toArgb(),
                        hsv
                    )
                    var primeraCarga by remember {
                        mutableStateOf(true)
                    }
                    Text("Brillo: ${brillo.toInt()}")

                    Slider(
                        value = brillo,
                        onValueChange = {

                            brillo = it

                            prefs.edit()
                                .putFloat("brillo", brillo)
                                .apply()

                            sendBrightness(brillo.toInt())
                        },
                        valueRange = 0f..80f,
                        colors = SliderDefaults.colors(
                            thumbColor = Dark,
                            activeTrackColor = Cream,
                            inactiveTrackColor = SoftPink
                        )
                    )

                    Spacer(Modifier.height(12.dp))






                    Spacer(Modifier.height(12.dp))


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                    )

                    Spacer(Modifier.height(20.dp))

                    Text("Favoritos")
                    Text("Selecciona un color del espectro y luego manten presionado un cuadro de color por unos segundos. Disfruta tu nuevo color!")

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        favoritos.forEachIndexed { index, favorito ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        favorito ?: Color.Gray.copy(alpha = 0.3f)
                                    )
                                    .combinedClickable(

                                        onClick = {

                                            favorito?.let { color ->

                                                currentColor = color

                                                prefs.edit()
                                                    .putInt(
                                                        "current_color",
                                                        color.toArgb()
                                                    )
                                                    .apply()

                                                sendColor(
                                                    (color.red * 255).toInt(),
                                                    (color.green * 255).toInt(),
                                                    (color.blue * 255).toInt()
                                                )
                                            }

                                        },

                                        onLongClick = {

                                            favoritos = favoritos.toMutableList().also {
                                                it[index] = currentColor
                                            }

                                            prefs.edit()
                                                .putInt(
                                                    "favorito_$index",
                                                    currentColor.toArgb()
                                                )
                                                .apply()

                                        }

                                    )
                            )

                        }

                    }
                    Spacer(Modifier.height(12.dp))


                    ColorPicker(
                        currentColor = currentColor,

                        onColorChanged = { color ->

                            currentColor = color

                            prefs.edit()
                                .putInt(
                                    "current_color",
                                    color.toArgb()
                                )
                                .apply()

                        },

                        sendColor = { r, g, b ->

                            sendColor(r, g, b)

                        }
                    )


                    DinoButton("Wine") {
                        // Original: (343, 44, 25) -> ¡Ojo! El rojo estaba en 343, el máximo es 255.
                        // Lo corregimos a su máxima expresión proporcional:
                        sendColor(255, 33, 19)
                    }

                    DinoButton("Champagne") {
                        // Original: (252, 228, 216) -> Ya es un color muy claro y pastel.
                        // Si lo subimos más se vuelve blanco, así que este rango está perfecto:
                        sendColor(252, 228, 216)
                    }

                    DinoButton("French Rose") {
                        // Original: (247, 85, 144) -> Ya tiene excelente fuerza en el rojo (247).
                        // Lo dejamos igual para no saturarlo de más:
                        sendColor(247, 85, 144)
                    }

                    DinoButton("Crimson Silk") {
                        // Original: (215, 38, 56) -> Ya tiene buena intensidad en rojo.
                        // Lo dejamos tal cual para que mantenga ese tono carmesí:
                        sendColor(215, 38, 56)
                    }

                    DinoButton("Deep Bordeaux") {
                        // Original: (63, 15, 18) -> Muy bajo.
                        // Multiplicamos x3 para darle presencia en la tira sin perder el tono vino oscuro:
                        sendColor(189, 45, 54)
                    }

                    DinoButton("Deep Purple") {
                        // Original: (37, 9, 46) -> Extremadamente bajo, por eso se perdía o temblaba.
                        // Multiplicamos x4 para que el morado tenga un cuerpo y una saturación increíbles:
                        sendColor(148, 36, 184)
                    }
                }

                "modes" -> Column {

                    DinoCard("💡 Modos de Luz")

                    Spacer(Modifier.height(20.dp))
                    var brilloModos by remember {
                        mutableStateOf(prefs.getFloat("brillo_modos", 60f))
                    }
                    LaunchedEffect(Unit) {

                        if (modoActual != 0) {

                            sendMode(modoActual)
                            sendBrightness(brilloModos.toInt()) // Enviamos su brillo al entrar

                        }

                    }
                    Text("Brillo de Modos: ${brilloModos.toInt()}%")
                    Slider(
                        value = brilloModos,
                        onValueChange = {
                            brilloModos = it
                            prefs.edit().putFloat("brillo_modos", it).apply()
                            sendBrightness(it.toInt())
                        },
                        valueRange = 0f..80f
                    )

                    Spacer(Modifier.height(12.dp))

                    DinoButton("🌋 Lava") {

                        modoActual = 3
                        dinoEncendido = true

                        prefs.edit()
                            .putInt("modo_actual", 3)
                            .putBoolean("dino_encendido", true)
                            .apply()

                        sendMode(3)
                        sendBrightness(brilloModos.toInt())
                    }

                    DinoButton("🌈 Arcoiris") {

                        modoActual = 4
                        dinoEncendido = true

                        prefs.edit()
                            .putInt("modo_actual", 4)
                            .putBoolean("dino_encendido", true)
                            .apply()

                        sendMode(4)
                        sendBrightness(brilloModos.toInt())

                    }

                    DinoButton("❤️ Respirar") {

                        modoActual = 1
                        dinoEncendido = true

                        prefs.edit()
                            .putInt("modo_actual", 1)
                            .putBoolean("dino_encendido", true)
                            .apply()

                        sendMode(1)
                        sendBrightness(brilloModos.toInt())

                    }


                }

                "scenes" -> Column {
                    var escenaEliminar by remember {

                        mutableStateOf<Escena?>(null)

                    }
                    var creandoEscena by remember {
                        mutableStateOf(false)
                    }
                    var escenaEditando by remember {
                        mutableStateOf<Escena?>(null)
                    }



                    DinoCard("🎬 Escenas")

                    val escenas = remember {

                        mutableStateOf(
                            SceneManager.cargarTodasLasEscenas(context)
                        )

                    }

                    escenas.value.forEachIndexed { index, escena ->

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            DinoButton(
                                "🎬 ${escena.nombre}"
                            ) {

                                aplicarEscena(escena)

                            }

                            DinoSmallButton("✏️") {

                                escenaEditando = escena
                                creandoEscena = true

                            }

                            DinoSmallButton("🗑️") {

                                escenaEliminar = escena

                            }

                        }

                    }
                    if (escenaEliminar != null) {

                        AlertDialog(

                            onDismissRequest = {

                                escenaEliminar = null

                            },

                            title = {

                                Text("Eliminar escena")

                            },

                            text = {

                                Text(

                                    "¿Eliminar \"${escenaEliminar!!.nombre}\"?"

                                )

                            },

                            confirmButton = {

                                TextButton(

                                    onClick = {

                                        SceneManager.eliminarEscena(

                                            context,

                                            escenaEliminar!!.id

                                        )

                                        escenas.value =

                                            SceneManager
                                                .cargarTodasLasEscenas(context)

                                        escenaEliminar = null

                                    }

                                ) {

                                    Text("Eliminar")

                                }

                            },

                            dismissButton = {

                                TextButton(

                                    onClick = {

                                        escenaEliminar = null

                                    }

                                ) {

                                    Text("Cancelar")

                                }

                            }

                        )

                    }
                    Spacer(Modifier.height(20.dp))

                    DinoButton("➕ Crear escena") {

                        creandoEscena = !creandoEscena

                    }
                    if (creandoEscena) {

                        var nombre by remember(escenaEditando) {

                            mutableStateOf(
                                escenaEditando?.nombre ?: ""
                            )

                        }

                        var brilloEscena by remember(escenaEditando) {

                            mutableStateOf(

                                escenaEditando?.brillo?.toFloat() ?: 50f

                            )

                        }

                        var efecto by remember(escenaEditando) {

                            mutableStateOf(

                                escenaEditando?.efecto ?: Efecto.ESTATICO

                            )

                        }

                        var coloresEscena by remember(escenaEditando, efecto) {
                            // 1. Primero creamos la constante local fija aquí dentro del remember
                            val escenaActual = escenaEditando

                            // 2. Ahora sí inicializamos el mutableStateOf de forma limpia
                            mutableStateOf(
                                if (escenaActual != null) {
                                    val listaExistente = escenaActual.colores
                                    MutableList(5) { index ->
                                        if (index < listaExistente.size) {
                                            listaExistente[index]
                                        } else {
                                            Color.Transparent.toArgb()
                                        }
                                    }
                                } else {
                                    MutableList(5) { Color.Transparent.toArgb() }
                                }
                            )
                        }
                        var colorSeleccionado by remember {

                            mutableStateOf(0)

                        }

                        OutlinedTextField(

                            value = nombre,

                            onValueChange = {

                                nombre = it

                            },

                            label = {

                                Text("Nombre")

                            }

                        )
                        Spacer(Modifier.height(12.dp))

                        Text("Efecto")
                        Column {

                            Efecto.entries.forEach {

                                DinoButton(it.name) {

                                    efecto = it
                                    colorSeleccionado = 0


                                }

                            }

                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Colores")

                        Spacer(Modifier.height(8.dp))
                        val cantidadColores = when (efecto) {

                            Efecto.ESTATICO -> 1

                            Efecto.RESPIRAR -> 1

                            Efecto.PARPADEO -> 2

                            Efecto.MEZCLA -> 5

                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            coloresEscena
                                .take(cantidadColores)
                                .forEachIndexed { index, color ->
                                    Box(

                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(

                                                if (color == Color.Transparent.toArgb())
                                                    Color.Gray.copy(alpha = 0.3f)
                                                else
                                                    Color(color)

                                            )
                                            .combinedClickable(

                                                onClick = {

                                                    colorSeleccionado = index

                                                }

                                            )

                                    )

                                }

                        }
                        Spacer(Modifier.height(12.dp))
//                    val colorActual = Color(coloresEscena[colorSeleccionado])
                        ColorPicker(
                            currentColor = Color(
                                coloresEscena[colorSeleccionado]
                            ),
                            onColorChanged = { nuevoColor ->

                                coloresEscena =
                                    coloresEscena.toMutableList().also {

                                        it[colorSeleccionado] =
                                            nuevoColor.toArgb()

                                    }

                            },

                            sendColor = { r, g, b ->

                                sendColor(r, g, b)

                            }

                        )
                        Text(
                            "Brillo: ${brilloEscena.toInt()}%"
                        )
                        Slider(

                            value = brilloEscena,

                            onValueChange = {

                                brilloEscena = it

                            },

                            valueRange = 0f..80f

                        )
                        DinoButton("💾 Guardar") {

                            val nuevaEscena = Escena(

                                id = escenaEditando?.id
                                    ?: System.currentTimeMillis(),

                                nombre = nombre,

                                efecto = efecto,

                                colores = coloresEscena.take(cantidadColores),

                                brillo = brilloEscena.toInt()

                            )

                            if (escenaEditando == null) {

                                SceneManager.agregarEscena(
                                    context,
                                    nuevaEscena
                                )

                            } else {

                                SceneManager.actualizarEscena(
                                    context,
                                    nuevaEscena
                                )

                            }

                            escenas.value =
                                SceneManager.cargarTodasLasEscenas(context)

                            escenaEditando = null
                            creandoEscena = false

                        }

                    }


                }
            }
        }


    }



    @Composable
    fun DinoButton(
        texto: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Beige,
                contentColor = Dark
            ),
            modifier = modifier
        ) {
            Text(texto)
        }
    }

    @Composable
    fun DinoSmallButton(
        texto: String,
        onClick: () -> Unit
    ) {

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Beige,
                contentColor = Dark
            )
        ) {

            Text(texto)

        }

    }

    @Composable
    fun DinoCard(text: String) {
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Beige
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                color = Dark
            )
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DinoHomePreview() {
        DinoCompanionAppTheme {
            DinoHome()
        }
    }
}
