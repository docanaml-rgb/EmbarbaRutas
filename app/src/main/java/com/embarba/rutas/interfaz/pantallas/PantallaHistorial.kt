package com.embarba.rutas.interfaz.pantallas

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embarba.rutas.R
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.interfaz.componentes.MensajeEstadoAplicacion
import com.embarba.rutas.interfaz.componentes.AppStatusMessageType
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val PERU_TIME_ZONE: TimeZone = TimeZone.getTimeZone("America/Lima")

private val HistoryBackground = Color(0xFFF6F8FC)
private val HistoryInstitutionalBlue = Color(0xFF103F78)
private val HistoryBrightBlue = Color(0xFF2675D9)
private val HistoryText = Color(0xFF172B4D)
private val HistorySecondaryText = Color(0xFF60758A)
private val HistoryFinishedBackground = Color(0xFFDCFCE7)
private val HistoryFinishedText = Color(0xFF166534)

@Composable
fun PantallaHistorial(
    supervisionRepository: RepositorioSupervisiones,
    onBackClick: () -> Unit,
    onSupervisionClick: (Supervision) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var supervisiones by remember { mutableStateOf<List<Supervision>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    val supervisionesFiltradas = selectedDateMillis?.let { selectedDate ->
        supervisiones.filter { supervision ->
            supervision.horaFinalizacion?.isOnSameLocalDay(selectedDate) == true
        }
    } ?: supervisiones

    LaunchedEffect(Unit) {
        supervisionRepository.obtenerSupervisionesFinalizadas { result ->
            isLoading = false
            result.onSuccess { supervisiones = it }
            result.onFailure {
                errorMessage = "No se pudo cargar el historial de supervisiones."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HistoryBackground)
            .safeDrawingPadding()
    ) {
        HistoryCorporateHeader(onBackClick = onBackClick)

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HistoryInstitutionalBlue)
            }
            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                MensajeEstadoAplicacion(
                    type = AppStatusMessageType.Error,
                    title = "No se pudo cargar el historial",
                    message = errorMessage.orEmpty()
                )
            }
            else -> {
                HistoryDateFilter(
                    selectedDateMillis = selectedDateMillis,
                    onSelectDate = {
                        val initialDate = Calendar.getInstance(PERU_TIME_ZONE).apply {
                            selectedDateMillis?.let { timeInMillis = it }
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedDateMillis = Calendar.getInstance(PERU_TIME_ZONE).apply {
                                    clear()
                                    set(year, month, dayOfMonth)
                                }.timeInMillis
                            },
                            initialDate.get(Calendar.YEAR),
                            initialDate.get(Calendar.MONTH),
                            initialDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    onClearDate = { selectedDateMillis = null }
                )

                when {
                    supervisiones.isEmpty() -> HistoryEmptyMessage("No tienes supervisiones finalizadas.")
                    supervisionesFiltradas.isEmpty() -> HistoryEmptyMessage(
                        "No hay supervisiones finalizadas en esta fecha"
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(supervisionesFiltradas, key = { it.id }) { supervision ->
                            HistorySupervisionCard(
                                supervision,
                                onClick = { onSupervisionClick(supervision) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCorporateHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .background(HistoryInstitutionalBlue)
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
                text = "Historial de supervisiones",
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
            drawPath(backWave, HistoryBrightBlue.copy(alpha = .48f))
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
            drawPath(frontWave, HistoryBackground)
        }
    }
}

@Composable
private fun HistoryDateFilter(
    selectedDateMillis: Long?,
    onSelectDate: () -> Unit,
    onClearDate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = onSelectDate,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, HistoryInstitutionalBlue),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = HistoryInstitutionalBlue
            )
        ) {
            Icon(Icons.Filled.CalendarMonth, null, Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                selectedDateMillis?.let {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                        timeZone = PERU_TIME_ZONE
                    }.format(it)
                    "Fecha: $date"
                } ?: "Filtrar por fecha de finalización",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (selectedDateMillis != null) {
            TextButton(onClick = onClearDate, modifier = Modifier.align(Alignment.End)) {
                Text("Limpiar filtro", color = HistoryInstitutionalBlue)
            }
        }
    }
}

@Composable
private fun HistoryEmptyMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = HistorySecondaryText)
    }
}

private fun Timestamp.isOnSameLocalDay(dateMillis: Long): Boolean {
    val completionDate = Calendar.getInstance(PERU_TIME_ZONE).apply { time = toDate() }
    val selectedDate = Calendar.getInstance(PERU_TIME_ZONE).apply { timeInMillis = dateMillis }
    return completionDate.get(Calendar.ERA) == selectedDate.get(Calendar.ERA) &&
        completionDate.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
        completionDate.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun HistorySupervisionCard(supervision: Supervision, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    color = HistoryFinishedText,
                    topLeft = Offset.Zero,
                    size = Size(5.dp.toPx(), size.height)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = supervision.id,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = HistoryInstitutionalBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = HistoryFinishedBackground,
                        contentColor = HistoryFinishedText,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = supervision.estado,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = supervision.cliente,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HistoryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Abrir detalle histórico",
                        tint = HistoryInstitutionalBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                HistoryInfoRow(Icons.Filled.LocationOn, supervision.direccion, 2)
                HistoryInfoRow(Icons.Filled.Apartment, supervision.distrito, 1)
                HistoryInfoRow(Icons.Filled.CalendarMonth, supervision.hora, 1)
            }
        }
    }
}

@Composable
private fun HistoryInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    maxLines: Int
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(18.dp), HistorySecondaryText)
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = HistorySecondaryText,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorySupervisionCardPreview() {
    EmbarbaRutasTheme {
        HistorySupervisionCard(
            Supervision("SUP-000", "Cliente de prueba", "Dirección", "Distrito", "10:00", "Finalizada", 0.0, 0.0)
        )
    }
}
