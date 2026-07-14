package com.example.dinocompanionapp

data class Escena(

    val id: Long = System.currentTimeMillis(),
    val nombre: String,

    val efecto: Efecto,

    val colores: List<Int>,

    val brillo: Int

)

enum class Efecto(val codigo:Int) {

    ESTATICO(0),

    RESPIRAR(1),
    MEZCLA(2),
    PARPADEO(3)

}