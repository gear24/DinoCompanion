package com.example.dinocompanionapp.data


data class Escena(
    val id: Long = System.currentTimeMillis(),
    val nombre: String,
    val efecto: EfectoEscena,
    val colores: List<Int>,
    val brillo: Int,
    val velocidad: Int = 50
)

enum class EfectoEscena(val codigo: Int) {
    ESTATICO(0),
    RESPIRAR(1),
    MEZCLA(2),
    PARPADEO(3)
}