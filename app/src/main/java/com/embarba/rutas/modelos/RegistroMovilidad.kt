package com.embarba.rutas.modelos

import com.google.firebase.Timestamp

data class RegistroMovilidad(
    val fechaRegistro: Timestamp?,
    val motivo: String,
    val medioTransporte: String,
    val especificacionMedioTransporte: String,
    val gastoTotal: Double,
    val justificacionSinGasto: String
)
