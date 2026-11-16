package com.embarba.rutas.modelos

import com.google.firebase.Timestamp

data class Supervision(
    val id: String,
    val cliente: String,
    val direccion: String,
    val distrito: String,
    val hora: String,
    val estado: String,
    val latitud: Double,
    val longitud: Double,
    val horaFinalizacion: Timestamp? = null
)
