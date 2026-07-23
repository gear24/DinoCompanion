package com.example.dinocompanionapp.ui.components


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Importa tus colores personalizados desde el paquete de tu tema
import com.example.dinocompanionapp.ui.theme.Beige
import com.example.dinocompanionapp.ui.theme.Dark

@Composable
fun DinoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
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