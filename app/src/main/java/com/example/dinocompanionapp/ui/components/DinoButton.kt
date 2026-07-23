package com.example.dinocompanionapp.ui.components


import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
// Importa tus colores personalizados desde el paquete de tu tema
import com.example.dinocompanionapp.ui.theme.Beige
import com.example.dinocompanionapp.ui.theme.Dark

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