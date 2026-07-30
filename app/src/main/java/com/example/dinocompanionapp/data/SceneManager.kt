package com.example.dinocompanionapp.data


import android.content.Context
import androidx.core.content.edit


object SceneManager {

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            "dino_scenes",
            Context.MODE_PRIVATE
        )

    fun guardarEscena(
        context: Context,
        indice: Int,
        escena: Escena
    ) {
        prefs(context).edit {

            putLong("escena_${indice}_id", escena.id)
            putString("escena_${indice}_nombre", escena.nombre)
            putString("escena_${indice}_efecto", escena.efecto.name)
            putInt("escena_${indice}_brillo", escena.brillo)
            putInt("escena_${indice}_velocidad", escena.velocidad)
            putInt("escena_${indice}_total_colores", escena.colores.size)

            escena.colores.forEachIndexed { index, color ->
                putInt("escena_${indice}_color_$index", color)
            }
        }
    }

    fun cargarEscena(
        context: Context,
        indice: Int
    ): Escena? {
        val prefs = prefs(context)
        val nombre = prefs.getString("escena_${indice}_nombre", null) ?: return null
        val id = prefs.getLong("escena_${indice}_id", -1L)

        // 👈 Usamos EfectoEscena en lugar de Efecto
        val efecto = EfectoEscena.valueOf(
            prefs.getString(
                "escena_${indice}_efecto",
                EfectoEscena.ESTATICO.name
            )!!
        )

        val brillo = prefs.getInt("escena_${indice}_brillo", 50)
        val velocidad = prefs.getInt("escena_${indice}_velocidad", 50) // 👈 Leemos velocidad (default 50)
        val totalColores = prefs.getInt("escena_${indice}_total_colores", 0)

        val colores = mutableListOf<Int>()
        repeat(totalColores) { index ->
            colores.add(prefs.getInt("escena_${indice}_color_$index", 0))
        }

        return Escena(
            id = id,
            nombre = nombre,
            efecto = efecto,
            colores = colores,
            brillo = brillo,
            velocidad = velocidad // 👈 Construimos la escena con la velocidad
        )
    }

    fun guardarCantidadEscenas(context: Context, cantidad: Int) {
        prefs(context).edit {
            putInt("cantidad_escenas", cantidad)
        }
    }

    fun obtenerCantidadEscenas(context: Context): Int {
        return prefs(context).getInt("cantidad_escenas", 0)
    }

    fun cargarTodasLasEscenas(context: Context): List<Escena> {
        val escenas = mutableListOf<Escena>()
        val cantidad = obtenerCantidadEscenas(context)

        repeat(cantidad) { index ->
            cargarEscena(context, index)?.let {
                escenas.add(it)
            }
        }
        return escenas
    }

    fun agregarEscena(context: Context, escena: Escena) {
        val indice = obtenerCantidadEscenas(context)
        guardarEscena(context, indice, escena)
        guardarCantidadEscenas(context, indice + 1)
    }

    fun actualizarEscena(context: Context, escena: Escena) {
        val escenas = cargarTodasLasEscenas(context).toMutableList()
        val index = escenas.indexOfFirst { it.id == escena.id }

        if (index != -1) {
            escenas[index] = escena
        }

        guardarTodasLasEscenas(context, escenas)
    }

    fun guardarTodasLasEscenas(context: Context, escenas: List<Escena>) {
        guardarCantidadEscenas(context, escenas.size)
        escenas.forEachIndexed { index, escena ->
            guardarEscena(context, index, escena)
        }
    }

    fun eliminarEscena(context: Context, id: Long) {
        val escenas = cargarTodasLasEscenas(context).filter { it.id != id }
        guardarTodasLasEscenas(context, escenas)
    }
}