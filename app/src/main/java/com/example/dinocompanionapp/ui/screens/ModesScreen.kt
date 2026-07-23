package com.example.dinocompanionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.theme.*
import com.example.dinocompanionapp.viewmodel.DinoViewModel

@Composable
fun ModesScreen(
    animState: Boolean,       // Indica si hay alguna animación corriendo
    speed: Float,             // Velocidad actual del slider (ej. 1f..100f)
    onSpeedChanged: (Float) -> Unit,
    onStartLava: () -> Unit,
    onStartArcoiris: () -> Unit,
    onStartRespirar: () -> Unit,
    onStartForest: ()-> Unit,
    onStartParty: ()-> Unit,
    onStartOcean: ()-> Unit,
    onStopAnimation: () -> Unit,
    onBackToHome: () -> Unit,
    onReactivarModo: () -> Unit, // 👈 Nuevo callback
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onReactivarModo()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Pink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DinoButton("⬅️ Volver al Home") {
            onBackToHome()
        }

        Spacer(Modifier.height(16.dp))

        DinoCard("Efectos Animados")

        Spacer(Modifier.height(20.dp))

        Text("Velocidad de Efecto: ${speed.toInt()}")

        Slider(
            value = speed,
            onValueChange = onSpeedChanged,
            valueRange = 1f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Dark,
                activeTrackColor = Cream,
                inactiveTrackColor = SoftPink
            )
        )

        Spacer(Modifier.height(24.dp))

        // Botón de encendido/apagado general del efecto
        if (animState) {
            DinoButton("🛑 Apagar Efecto") {
                onStopAnimation()
            }
        } else {
            Text("No hay ningún efecto activo en este momento.")
        }

        Spacer(Modifier.height(24.dp))

        Text("Selecciona una animación:")
        Spacer(Modifier.height(12.dp))

        DinoButton("🌋 Efecto Lava") {
            onStartLava()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton("🌈 Arcoíris") {
            onStartArcoiris()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton("💨 Efecto Respirar") {
            onStartRespirar()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton(" Efecto Oceano") {
            onStartOcean()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton(" Efecto Bosque") {
            onStartForest()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton(" Efecto Fiesta") {
            onStartParty()
        }

    }
}