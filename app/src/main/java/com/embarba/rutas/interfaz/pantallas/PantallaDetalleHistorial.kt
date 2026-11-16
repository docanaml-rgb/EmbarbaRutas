package com.embarba.rutas.interfaz.pantallas

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embarba.rutas.R
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.datos.RepositorioUsuarios
import com.embarba.rutas.modelos.ChecklistResult
import com.embarba.rutas.modelos.DetalleHistorial
import com.embarba.rutas.modelos.RegistroMovilidad
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.reportes.GeneradorPdfSupervision
import com.embarba.rutas.interfaz.componentes.MensajeEstadoAplicacion
import com.embarba.rutas.interfaz.componentes.AppStatusMessageType
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme
import com.google.firebase.Timestamp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val PERU_TIME_ZONE: TimeZone = TimeZone.getTimeZone("America/Lima")

private val HistoryDetailBackground = Color(0xFFF6F8FC)
private val HistoryDetailInstitutionalBlue = Color(0xFF103F78)
private val HistoryDetailBrightBlue = Color(0xFF2675D9)
private val HistoryDetailText = Color(0xFF172B4D)
private val HistoryDetailSecondaryText = Color(0xFF60758A)
private val HistoryDetailSuccessBackground = Color(0xFFDCFCE7)
private val HistoryDetailSuccessText = Color(0xFF166534)
private val HistoryDetailFailureBackground = Color(0xFFFEE2E2)
private val HistoryDetailFailureText = Color(0xFF991B1B)

@Composable
fun PantallaDetalleHistorial(
    supervision: Supervision,
    supervisionRepository: RepositorioSupervisiones,
    usuarioRepository: RepositorioUsuarios,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var detalle by remember { mutableStateOf<DetalleHistorial?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGeneratingReport by remember { mutableStateOf(false) }
    var reportErrorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(supervision.id) {
        supervisionRepository.obtenerDetalleHistorial(supervision.id) { result ->
            isLoading = false
            result.onSuccess { detalle = it }
            result.onFailure { errorMessage = "No se pudo cargar el detalle de la supervisión." }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HistoryDetailBackground)
            .safeDrawingPadding()
    ) {
        HistoryDetailHeader(onBackClick = onBackClick)

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HistoryDetailInstitutionalBlue)
            }
            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                MensajeEstadoAplicacion(
                    type = AppStatusMessageType.Error,
                    title = "No se pudo cargar el detalle",
                    message = errorMessage.orEmpty()
                )
            }
            detalle != null -> HistoryDetailContent(
                supervision = supervision,
                detalle = detalle!!,
                isGeneratingReport = isGeneratingReport,
                reportErrorMessage = reportErrorMessage,
                onGenerateReport = {
                    if (isGeneratingReport) return@HistoryDetailContent
                    isGeneratingReport = true
                    reportErrorMessage = null
                    usuarioRepository.obtenerUsuarioActual { userResult ->
                        userResult.onSuccess { usuario ->
                            coroutineScope.launch {
                                val reportResult = withContext(Dispatchers.IO) {
                                    runCatching {
                                        GeneradorPdfSupervision.generate(
                                            context = context.applicationContext,
                                            supervision = supervision,
                                            detail = detalle!!,
                                            supervisorName = usuario.nombre
                                        )
                                    }
                                }
                                isGeneratingReport = false
                                reportResult.onSuccess { file ->
                                    runCatching { sharePdf(context, file) }
                                        .onFailure {
                                            reportErrorMessage =
                                                "No se encontró una aplicación compatible para compartir el PDF."
                                        }
                                }
                                reportResult.onFailure { error ->
                                    reportErrorMessage = error.message
                                        ?: "No se pudo generar el informe PDF."
                                }
                            }
                        }
                        userResult.onFailure {
                            isGeneratingReport = false
                            reportErrorMessage =
                                "No se pudo obtener el nombre del supervisor desde su perfil."
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HistoryDetailHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .background(HistoryDetailInstitutionalBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 12.dp, end = 20.dp, bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Regresar",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Detalle de supervisión",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .align(Alignment.BottomCenter)
        ) {
            val backWave = Path().apply {
                moveTo(0f, size.height * .4f)
                cubicTo(
                    size.width * .28f, 0f,
                    size.width * .68f, size.height * .88f,
                    size.width, size.height * .3f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(backWave, HistoryDetailBrightBlue.copy(alpha = .48f))
            val frontWave = Path().apply {
                moveTo(0f, size.height * .72f)
                cubicTo(
                    size.width * .32f, size.height * .36f,
                    size.width * .74f, size.height,
                    size.width, size.height * .58f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(frontWave, HistoryDetailBackground)
        }
    }
}

@Composable
private fun HistoryDetailContent(
    supervision: Supervision,
    detalle: DetalleHistorial,
    isGeneratingReport: Boolean = false,
    reportErrorMessage: String? = null,
    onGenerateReport: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SupervisionHistoryCard(supervision, detalle)

        SectionTitle(Icons.AutoMirrored.Filled.ReceiptLong, "Gastos de movilidad")
        if (detalle.movilidad == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Sin registro de movilidad",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = HistoryDetailSecondaryText
                )
            }
        } else {
            MovilidadResultContent(detalle.movilidad)
        }

        Text(
            "Resultado del checklist",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HistoryDetailInstitutionalBlue
        )
        if (detalle.checklist == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    "No se encontró el checklist de esta supervisión.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = HistoryDetailSecondaryText
                )
            }
        } else {
            ChecklistResultContent(detalle.checklist)
        }

        if (supervision.estado == "Finalizada" &&
            GeneradorPdfSupervision.isValidChecklist(detalle.checklist)
        ) {
            Button(
                onClick = onGenerateReport,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HistoryDetailInstitutionalBlue,
                    contentColor = Color.White
                ),
                enabled = !isGeneratingReport
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isGeneratingReport) "GENERANDO INFORME..." else "GENERAR INFORME PDF",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        reportErrorMessage?.let { message ->
            MensajeEstadoAplicacion(
                type = AppStatusMessageType.Error,
                title = "No se pudo generar el informe",
                message = message
            )
        }
    }
}

@Composable
private fun SupervisionHistoryCard(supervision: Supervision, detalle: DetalleHistorial) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    color = HistoryDetailSuccessText,
                    topLeft = Offset.Zero,
                    size = Size(5.dp.toPx(), size.height)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Assignment,
                        null,
                        Modifier.size(23.dp),
                        HistoryDetailInstitutionalBlue
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Código de supervisión",
                            style = MaterialTheme.typography.labelMedium,
                            color = HistoryDetailSecondaryText
                        )
                        Text(
                            supervision.id,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HistoryDetailInstitutionalBlue,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = HistoryDetailSuccessBackground,
                        contentColor = HistoryDetailSuccessText,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            supervision.estado,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE4EAF1))
                HistoryDetailField(Icons.Filled.Person, "Cliente", supervision.cliente, 2)
                HorizontalDivider(color = Color(0xFFE4EAF1))
                HistoryDetailField(Icons.Filled.LocationOn, "Dirección", supervision.direccion, 3)
                HorizontalDivider(color = Color(0xFFE4EAF1))
                HistoryDetailField(Icons.Filled.Apartment, "Distrito", supervision.distrito, 2)
                HorizontalDivider(color = Color(0xFFE4EAF1))
                HistoryDetailField(Icons.Filled.CalendarMonth, "Hora programada", supervision.hora, 1)
                HorizontalDivider(color = Color(0xFFE4EAF1))
                HistoryDetailField(Icons.Filled.Schedule, "Hora de llegada", formatTimestamp(detalle.horaLlegada), 2)
                HorizontalDivider(color = Color(0xFFE4EAF1))
                HistoryDetailField(
                    Icons.Filled.EventAvailable,
                    "Hora de finalización",
                    formatTimestamp(detalle.horaFinalizacion),
                    2
                )
            }
        }
    }
}

@Composable
private fun HistoryDetailField(icon: ImageVector, label: String, value: String, maxLines: Int) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), HistoryDetailInstitutionalBlue)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = HistoryDetailSecondaryText)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = HistoryDetailText,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), HistoryDetailInstitutionalBlue)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HistoryDetailInstitutionalBlue
        )
    }
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Compartir informe PDF"))
    } catch (error: ActivityNotFoundException) {
        throw error
    }
}

@Composable
private fun MovilidadResultContent(movilidad: RegistroMovilidad) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MobilityHistoryField("Fecha", formatTimestamp(movilidad.fechaRegistro))
            HorizontalDivider(color = Color(0xFFE4EAF1))
            MobilityHistoryField("Motivo", movilidad.motivo)
            HorizontalDivider(color = Color(0xFFE4EAF1))
            MobilityHistoryField("Medio de transporte", movilidad.medioTransporte)
            if (movilidad.medioTransporte == "Otro") {
                HorizontalDivider(color = Color(0xFFE4EAF1))
                MobilityHistoryField(
                    "Especificación del medio de transporte",
                    movilidad.especificacionMedioTransporte
                )
            }
            HorizontalDivider(color = Color(0xFFE4EAF1))
            MobilityHistoryField("Gasto total", String.format(Locale.US, "S/ %.2f", movilidad.gastoTotal))
            if (movilidad.gastoTotal == 0.0) {
                HorizontalDivider(color = Color(0xFFE4EAF1))
                MobilityHistoryField("Justificación sin gasto", movilidad.justificacionSinGasto)
            }
        }
    }
}

@Composable
private fun MobilityHistoryField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = HistoryDetailSecondaryText)
        Text(
            value.ifBlank { "No disponible" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = HistoryDetailText
        )
    }
}

@Composable
private fun ChecklistResultContent(checklist: ChecklistResult) {
    val verificaciones = listOf(
        Triple("limpiezaConservacionCabina", "1. Limpieza y conservación de la cabina", checklist.limpiezaConservacionCabina),
        Triple("iluminacionCabina", "2. Iluminación interior de la cabina", checklist.iluminacionCabina),
        Triple("botonesPanelControl", "3. Botones del panel de control", checklist.botonesPanelControl),
        Triple("aperturaCierrePuertas", "4. Apertura y cierre de puertas", checklist.aperturaCierrePuertas),
        Triple("deteccionObstaculosPuertas", "5. Detección de obstáculos", checklist.deteccionObstaculosPuertas),
        Triple("nivelacionPisos", "6. Nivelación en cada piso", checklist.nivelacionPisos),
        Triple("ruidosVibraciones", "7. Ausencia de ruidos o vibraciones anormales", checklist.ruidosVibraciones),
        Triple("indicadoresPisoDireccion", "8. Indicadores de piso y dirección", checklist.indicadoresPisoDireccion),
        Triple("botonesLlamadaPisos", "9. Botones de llamada en los pisos", checklist.botonesLlamadaPisos),
        Triple("alarmaComunicacionEmergencia", "10. Alarma o comunicación de emergencia", checklist.alarmaComunicacionEmergencia),
        Triple("senalizacionSeguridad", "11. Señalización e instrucciones de seguridad", checklist.senalizacionSeguridad),
        Triple("accesosLibresObstaculos", "12. Accesos libres de obstáculos", checklist.accesosLibresObstaculos)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        verificaciones.forEach { (campo, texto, respuesta) ->
            ChecklistResultItem(
                label = texto,
                result = respuesta,
                incidentObservation = if (respuesta == "No cumple") {
                    checklist.observacionesIncidencias[campo].orEmpty()
                } else {
                    null
                }
            )
        }
        HistoryTextCard("Observaciones generales", checklist.observaciones.ifBlank { "Sin observaciones." })
        HistoryTextCard("Fecha de registro", formatTimestamp(checklist.fechaRegistro))
    }
}

@Composable
private fun ChecklistResultItem(label: String, result: String, incidentObservation: String?) {
    val isCompliant = result == "Cumple"
    val isNonCompliant = result == "No cumple"
    val resultBackground = when {
        isCompliant -> HistoryDetailSuccessBackground
        isNonCompliant -> HistoryDetailFailureBackground
        else -> Color(0xFFDCEBFA)
    }
    val resultColor = when {
        isCompliant -> HistoryDetailSuccessText
        isNonCompliant -> HistoryDetailFailureText
        else -> HistoryDetailInstitutionalBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HistoryDetailText
                )
                Spacer(Modifier.width(8.dp))
                Surface(color = resultBackground, contentColor = resultColor, shape = RoundedCornerShape(50)) {
                    Text(
                        result.ifBlank { "No disponible" },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (incidentObservation != null) {
                HorizontalDivider(color = Color(0xFFE4EAF1))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Observación de la incidencia",
                        style = MaterialTheme.typography.labelMedium,
                        color = HistoryDetailFailureText
                    )
                    Text(
                        incidentObservation.ifBlank { "No disponible" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = HistoryDetailText
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTextCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = HistoryDetailInstitutionalBlue
            )
            Text(
                value.ifBlank { "No disponible" },
                style = MaterialTheme.typography.bodyLarge,
                color = HistoryDetailText
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    return timestamp?.toDate()?.let { date ->
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
            timeZone = PERU_TIME_ZONE
        }.format(date)
    } ?: "No disponible"
}

@Preview(showBackground = true)
@Composable
private fun HistoryDetailContentPreview() {
    EmbarbaRutasTheme {
        HistoryDetailContent(
            supervision = Supervision("SUP-000", "Cliente de prueba", "Dirección", "Distrito", "10:00", "Finalizada", 0.0, 0.0),
            detalle = DetalleHistorial(null, null, null)
        )
    }
}
