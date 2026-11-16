package com.embarba.rutas.interfaz.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embarba.rutas.R
import com.embarba.rutas.datos.MovilidadYaRegistradaException
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.modelos.RegistroMovilidad
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.interfaz.componentes.MensajeEstadoAplicacion
import com.embarba.rutas.interfaz.componentes.AppStatusMessageType
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MOTIVO_MOVILIDAD = "Supervisión de ascensor"
private val PERU_TIME_ZONE: TimeZone = TimeZone.getTimeZone("America/Lima")

private val MobilityBackground = Color(0xFFF6F8FC)
private val MobilityInstitutionalBlue = Color(0xFF103F78)
private val MobilityBrightBlue = Color(0xFF2675D9)
private val MobilityText = Color(0xFF172B4D)
private val MobilitySecondaryText = Color(0xFF60758A)

private val mediosTransporte = listOf(
    "Transporte público",
    "Taxi/aplicativo",
    "Movilidad propia",
    "Otro"
)

@Composable
fun PantallaMovilidad(
    supervision: Supervision,
    supervisionRepository: RepositorioSupervisiones,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var medioTransporte by rememberSaveable { mutableStateOf("") }
    var especificacionMedioTransporte by rememberSaveable { mutableStateOf("") }
    var gastoTotalTexto by rememberSaveable { mutableStateOf("") }
    var justificacionSinGasto by rememberSaveable { mutableStateOf("") }
    var fechaRegistro by remember { mutableStateOf<Timestamp?>(null) }
    var registroGuardado by rememberSaveable { mutableStateOf(false) }
    var isChecking by rememberSaveable { mutableStateOf(true) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var validarCampos by rememberSaveable { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var tipoMensaje by rememberSaveable { mutableStateOf(AppStatusMessageType.Info) }

    val gastoTotal = gastoTotalTexto.trim().replace(',', '.').toDoubleOrNull()
    val gastoValido = gastoTotal != null && gastoTotal.isFinite() && gastoTotal >= 0.0
    val requiereEspecificarMedio = medioTransporte == "Otro"
    val requiereJustificacion = gastoTotal == 0.0
    val formularioValido = medioTransporte.isNotBlank() &&
        (!requiereEspecificarMedio || especificacionMedioTransporte.isNotBlank()) &&
        gastoValido &&
        (!requiereJustificacion || justificacionSinGasto.isNotBlank())

    fun aplicarRegistro(registro: RegistroMovilidad) {
        fechaRegistro = registro.fechaRegistro
        medioTransporte = registro.medioTransporte
        especificacionMedioTransporte = registro.especificacionMedioTransporte
        gastoTotalTexto = if (registro.gastoTotal.isFinite()) {
            String.format(Locale.US, "%.2f", registro.gastoTotal)
        } else {
            ""
        }
        justificacionSinGasto = registro.justificacionSinGasto
        registroGuardado = true
    }

    fun cargarRegistro(mostrarMensaje: Boolean = true) {
        supervisionRepository.obtenerMovilidad(supervision.id) { result ->
            isChecking = false
            result.onSuccess { registro ->
                if (registro != null) {
                    aplicarRegistro(registro)
                    if (mostrarMensaje) {
                        mensaje = "La movilidad fue registrada"
                        tipoMensaje = AppStatusMessageType.Info
                    }
                }
            }
            result.onFailure {
                mensaje = "No se pudo verificar el registro de movilidad. Inténtalo nuevamente."
                tipoMensaje = AppStatusMessageType.Error
            }
        }
    }

    LaunchedEffect(supervision.id) {
        cargarRegistro()
    }

    val camposHabilitados = !isChecking && !isSaving && !registroGuardado

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MobilityBackground)
            .safeDrawingPadding()
    ) {
        MobilityCorporateHeader(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MovilidadHeader(supervision)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Datos del desplazamiento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MobilityText
                    )
                    ReadOnlyField(
                        label = "Fecha de registro",
                        value = fechaRegistro?.let(::formatMovilidadDate) ?: formatCurrentDate()
                    )
                    HorizontalDivider(color = Color(0xFFE4EAF1))
                    ReadOnlyField("Motivo", MOTIVO_MOVILIDAD)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Medio de transporte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MobilityText
                    )
                    mediosTransporte.forEach { medio ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (medioTransporte == medio) {
                                        MobilityBrightBlue
                                    } else {
                                        Color(0xFFCBD5E1)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    color = if (medioTransporte == medio) {
                                        Color(0xFFE0F2FE)
                                    } else {
                                        Color.White
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .selectable(
                                    selected = medioTransporte == medio,
                                    enabled = camposHabilitados,
                                    role = Role.RadioButton,
                                    onClick = {
                                        medioTransporte = medio
                                        if (medio != "Otro") {
                                            especificacionMedioTransporte = ""
                                        }
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = medioTransporte == medio,
                                onClick = null,
                                enabled = camposHabilitados,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MobilityBrightBlue,
                                    unselectedColor = Color(0xFF64748B),
                                    disabledSelectedColor = MobilityBrightBlue.copy(alpha = .5f),
                                    disabledUnselectedColor = Color(0xFF94A3B8)
                                )
                            )
                            Text(
                                medio,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (medioTransporte == medio) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (medioTransporte == medio) MobilityInstitutionalBlue else MobilityText
                            )
                        }
                    }
                    if (requiereEspecificarMedio) {
                        OutlinedTextField(
                            value = especificacionMedioTransporte,
                            onValueChange = { especificacionMedioTransporte = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Especifica el medio de transporte") },
                            supportingText = {
                                if (validarCampos && especificacionMedioTransporte.isBlank()) {
                                    Text("Especifica el medio de transporte utilizado.")
                                }
                            },
                            isError = validarCampos && especificacionMedioTransporte.isBlank(),
                            enabled = camposHabilitados,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = mobilityTextFieldColors()
                        )
                    }
                    if (validarCampos && medioTransporte.isBlank()) {
                        Text(
                            text = "Selecciona un medio de transporte.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gasto total de ida y vuelta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MobilityText
                    )
                    OutlinedTextField(
                        value = gastoTotalTexto,
                        onValueChange = { gastoTotalTexto = it },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("S/ ") },
                        placeholder = { Text("Importe total") },
                        supportingText = {
                            if (validarCampos && !gastoValido) {
                                Text("Ingresa un importe numérico, finito y no negativo.")
                            }
                        },
                        isError = validarCampos && !gastoValido,
                        enabled = camposHabilitados,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = mobilityTextFieldColors()
                    )
                    if (requiereJustificacion) {
                        OutlinedTextField(
                            value = justificacionSinGasto,
                            onValueChange = { justificacionSinGasto = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Justificación del gasto S/ 0.00") },
                            placeholder = { Text("Explica brevemente por qué no hubo gasto") },
                            supportingText = {
                                if (validarCampos && justificacionSinGasto.isBlank()) {
                                    Text("La justificación es obligatoria cuando el gasto es cero.")
                                }
                            },
                            isError = validarCampos && justificacionSinGasto.isBlank(),
                            enabled = camposHabilitados,
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = mobilityTextFieldColors()
                        )
                    }
                }
            }

            if (isChecking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (!registroGuardado) {
                Button(
                    onClick = {
                        validarCampos = true
                        if (!formularioValido) {
                            mensaje = "Completa correctamente todos los datos obligatorios."
                            tipoMensaje = AppStatusMessageType.Warning
                            return@Button
                        }

                        isSaving = true
                        mensaje = null
                        supervisionRepository.guardarMovilidad(
                            supervisionId = supervision.id,
                            motivo = MOTIVO_MOVILIDAD,
                            medioTransporte = medioTransporte,
                            especificacionMedioTransporte = especificacionMedioTransporte.trim(),
                            gastoTotal = requireNotNull(gastoTotal),
                            justificacionSinGasto = justificacionSinGasto.trim()
                        ) { result ->
                            isSaving = false
                            result.onSuccess {
                                registroGuardado = true
                                mensaje = "Movilidad registrada correctamente."
                                tipoMensaje = AppStatusMessageType.Success
                                cargarRegistro(mostrarMensaje = false)
                            }
                            result.onFailure { error ->
                                if (error.hasCause<MovilidadYaRegistradaException>()) {
                                    isChecking = true
                                    cargarRegistro(mostrarMensaje = false)
                                    mensaje = "La movilidad ya había sido registrada y no se sobrescribió."
                                    tipoMensaje = AppStatusMessageType.Info
                                } else {
                                    mensaje = "No se pudo guardar la movilidad. Inténtalo nuevamente."
                                    tipoMensaje = AppStatusMessageType.Error
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MobilityInstitutionalBlue,
                        contentColor = Color.White
                    ),
                    enabled = camposHabilitados
                ) {
                    Icon(Icons.Filled.Save, null, Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isSaving) "GUARDANDO MOVILIDAD..." else "GUARDAR MOVILIDAD",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            mensaje?.let {
                MensajeEstadoAplicacion(
                    type = tipoMensaje,
                    title = movilidadMessageTitle(tipoMensaje),
                    message = it
                )
            }
        }
    }
}

@Composable
private fun MobilityCorporateHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .background(MobilityInstitutionalBlue)
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
                text = "Movilidad",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
            drawPath(backWave, MobilityBrightBlue.copy(alpha = .48f))
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
            drawPath(frontWave, MobilityBackground)
        }
    }
}

@Composable
private fun MovilidadHeader(supervision: Supervision) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MovilidadHeaderField(Icons.AutoMirrored.Filled.Assignment, "Código de supervisión", supervision.id)
            HorizontalDivider(color = Color(0xFFE4EAF1))
            MovilidadHeaderField(Icons.Filled.Person, "Cliente", supervision.cliente)
            HorizontalDivider(color = Color(0xFFE4EAF1))
            MovilidadHeaderField(Icons.Filled.LocationOn, "Dirección", supervision.direccion)
            HorizontalDivider(color = Color(0xFFE4EAF1))
            MovilidadHeaderField(Icons.Filled.Apartment, "Distrito", supervision.distrito)
        }
    }
}

@Composable
private fun MovilidadHeaderField(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(22.dp), MobilityInstitutionalBlue)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MobilitySecondaryText)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MobilityText,
                maxLines = if (label == "Dirección") 3 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MobilitySecondaryText)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MobilityText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun mobilityTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = MobilityBrightBlue,
    unfocusedBorderColor = Color(0xFFCBD5E1),
    disabledBorderColor = Color(0xFFD4DFEB),
    focusedLabelColor = MobilityInstitutionalBlue,
    cursorColor = MobilityBrightBlue,
    focusedTextColor = MobilityText,
    unfocusedTextColor = MobilityText
)

private fun formatCurrentDate(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = PERU_TIME_ZONE
    }.format(Date())
}

private fun formatMovilidadDate(timestamp: Timestamp): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
        timeZone = PERU_TIME_ZONE
    }.format(timestamp.toDate())
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) return true
        current = current.cause
    }
    return false
}

private fun movilidadMessageTitle(type: AppStatusMessageType): String = when (type) {
    AppStatusMessageType.Success -> "Movilidad registrada"
    AppStatusMessageType.Error -> "No se pudo completar"
    AppStatusMessageType.Warning -> "Datos incompletos"
    AppStatusMessageType.Info -> "Información de movilidad"
}

@Preview(showBackground = true)
@Composable
private fun MovilidadScreenPreview() {
    EmbarbaRutasTheme {
        PantallaMovilidad(
            supervision = Supervision(
                id = "SUP-000",
                cliente = "Cliente de prueba",
                direccion = "Dirección de prueba",
                distrito = "Distrito de prueba",
                hora = "10:00",
                estado = "En supervision",
                latitud = 0.0,
                longitud = 0.0
            ),
            supervisionRepository = RepositorioSupervisiones(),
            onBackClick = {}
        )
    }
}
