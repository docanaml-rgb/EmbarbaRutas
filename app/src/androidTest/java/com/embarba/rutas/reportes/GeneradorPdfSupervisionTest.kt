package com.embarba.rutas.reportes

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.embarba.rutas.modelos.ChecklistResult
import com.embarba.rutas.modelos.DetalleHistorial
import com.embarba.rutas.modelos.RegistroMovilidad
import com.embarba.rutas.modelos.Supervision
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneradorPdfSupervisionTest {
    @Test
    fun generatesPdfForFinalizedSupervision() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val timestamp = Timestamp.now()
        val checklist = ChecklistResult(
            versionChecklist = 2,
            limpiezaConservacionCabina = "Cumple",
            iluminacionCabina = "Cumple",
            botonesPanelControl = "Cumple",
            aperturaCierrePuertas = "Cumple",
            deteccionObstaculosPuertas = "Cumple",
            nivelacionPisos = "Cumple",
            ruidosVibraciones = "Cumple",
            indicadoresPisoDireccion = "Cumple",
            botonesLlamadaPisos = "Cumple",
            alarmaComunicacionEmergencia = "Cumple",
            senalizacionSeguridad = "Cumple",
            accesosLibresObstaculos = "Cumple",
            observacionesIncidencias = emptyMap(),
            observaciones = "Supervisión completada sin incidencias.",
            fechaRegistro = timestamp
        )
        val detail = DetalleHistorial(
            horaLlegada = timestamp,
            horaFinalizacion = timestamp,
            checklist = checklist,
            movilidad = RegistroMovilidad(
                fechaRegistro = timestamp,
                motivo = "Supervisión de ascensor",
                medioTransporte = "Transporte público",
                especificacionMedioTransporte = "",
                gastoTotal = 12.5,
                justificacionSinGasto = ""
            )
        )
        val supervision = Supervision(
            id = "SUP-PDF-TEST",
            cliente = "Cliente de prueba",
            direccion = "Av. Pruebas 123",
            distrito = "Lima",
            hora = "09:00",
            estado = "Finalizada",
            latitud = -12.0464,
            longitud = -77.0428,
            horaFinalizacion = timestamp
        )

        val file = GeneradorPdfSupervision.generate(
            context = context,
            supervision = supervision,
            detail = detail,
            supervisorName = "Supervisor de prueba"
        )

        try {
            assertTrue(file.exists())
            assertTrue(file.length() > 1_000)
            assertEquals("%PDF", file.inputStream().buffered().use { input ->
                val header = ByteArray(4)
                input.read(header)
                String(header, Charsets.US_ASCII)
            })
        } finally {
            file.delete()
        }
    }
}
