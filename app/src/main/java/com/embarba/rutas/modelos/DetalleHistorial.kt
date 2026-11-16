package com.embarba.rutas.modelos

import com.google.firebase.Timestamp

data class DetalleHistorial(
    val horaLlegada: Timestamp?,
    val horaFinalizacion: Timestamp?,
    val checklist: ChecklistResult?,
    val movilidad: RegistroMovilidad? = null
)

data class ChecklistResult(
    val versionChecklist: Int,
    val limpiezaConservacionCabina: String,
    val iluminacionCabina: String,
    val botonesPanelControl: String,
    val aperturaCierrePuertas: String,
    val deteccionObstaculosPuertas: String,
    val nivelacionPisos: String,
    val ruidosVibraciones: String,
    val indicadoresPisoDireccion: String,
    val botonesLlamadaPisos: String,
    val alarmaComunicacionEmergencia: String,
    val senalizacionSeguridad: String,
    val accesosLibresObstaculos: String,
    val observacionesIncidencias: Map<String, String>,
    val observaciones: String,
    val fechaRegistro: Timestamp?
)
