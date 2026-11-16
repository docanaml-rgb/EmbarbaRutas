package com.embarba.rutas.interfaz.pantallas

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embarba.rutas.R
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.datos.ChecklistRequeridoException
import com.embarba.rutas.datos.MovilidadRequeridaException
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.interfaz.componentes.MensajeEstadoAplicacion
import com.embarba.rutas.interfaz.componentes.AppStatusMessageType
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme

private val ChecklistBackground = Color(0xFFF6F8FC)
private val ChecklistInstitutionalBlue = Color(0xFF103F78)
private val ChecklistBrightBlue = Color(0xFF2675D9)
private val ChecklistText = Color(0xFF172B4D)
private val ChecklistSecondaryText = Color(0xFF60758A)
private val ChecklistLightBlue = Color(0xFFDCEBFA)

private data class Verificacion(val campo: String, val texto: String, val categoria: String = "")

private val verificaciones = listOf(
    Verificacion("limpiezaConservacionCabina", "¿La cabina presenta condiciones adecuadas de limpieza y conservación?", "A. Estado de la cabina"),
    Verificacion("iluminacionCabina", "¿La iluminación interior de la cabina funciona correctamente?", "A. Estado de la cabina"),
    Verificacion("botonesPanelControl", "¿Los botones del panel de control funcionan correctamente?", "A. Estado de la cabina"),
    Verificacion("aperturaCierrePuertas", "¿Las puertas del ascensor abren y cierran correctamente?", "B. Puertas y desplazamiento"),
    Verificacion("deteccionObstaculosPuertas", "¿El sistema de detección de obstáculos de las puertas funciona correctamente?", "B. Puertas y desplazamiento"),
    Verificacion("nivelacionPisos", "¿El ascensor se detiene correctamente al nivel de cada piso?", "B. Puertas y desplazamiento"),
    Verificacion("ruidosVibraciones", "¿El ascensor se desplaza sin ruidos o vibraciones anormales?", "C. Funcionamiento general"),
    Verificacion("indicadoresPisoDireccion", "¿Los indicadores de piso y dirección funcionan correctamente?", "C. Funcionamiento general"),
    Verificacion("botonesLlamadaPisos", "¿Los botones de llamada en los pisos responden correctamente?", "C. Funcionamiento general"),
    Verificacion("alarmaComunicacionEmergencia", "¿El sistema de alarma o comunicación de emergencia funciona correctamente?", "D. Seguridad y comunicación"),
    Verificacion("senalizacionSeguridad", "¿La cabina cuenta con señalización e instrucciones de seguridad visibles?", "D. Seguridad y comunicación"),
    Verificacion("accesosLibresObstaculos", "¿Los accesos al ascensor se encuentran libres de obstáculos?", "D. Seguridad y comunicación")
)

@Composable
fun PantallaListaVerificacion(
    supervision: Supervision,
    supervisionRepository: RepositorioSupervisiones,
    onBackClick: () -> Unit,
    onMovilidadClick: (Supervision) -> Unit = {},
    onSupervisionFinalized: (Supervision) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val respuestas = remember { mutableStateMapOf<String, Boolean>() }
    val observacionesIncidencias = remember { mutableStateMapOf<String, String>() }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var tipoMensaje by rememberSaveable { mutableStateOf(AppStatusMessageType.Info) }
    var isCheckingChecklist by rememberSaveable { mutableStateOf(true) }
    var checklistVerificado by rememberSaveable { mutableStateOf(false) }
    var checklistGuardado by rememberSaveable { mutableStateOf(false) }
    var movilidadGuardada by rememberSaveable { mutableStateOf(false) }
    var requisitosVerificados by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var isFinalizing by rememberSaveable { mutableStateOf(false) }
    var showFinalizationDialog by rememberSaveable { mutableStateOf(false) }

    val verificacionesCompletadas = respuestas.size
    val progreso = verificacionesCompletadas.toFloat() / verificaciones.size
    val finalizacionHabilitada = requisitosVerificados && checklistGuardado && movilidadGuardada &&
        !isFinalizing && !showFinalizationDialog

    DisposableEffect(supervision.id) {
        val detenerObservacion = supervisionRepository.observarRequisitosFinalizacion(supervision.id) { result ->
            result.onSuccess { requisitos ->
                checklistGuardado = requisitos.checklistGuardado
                movilidadGuardada = requisitos.movilidadGuardada
                requisitosVerificados = true
            }
            result.onFailure {
                requisitosVerificados = false
                mensaje = "No se pudieron verificar los requisitos para finalizar. Inténtalo nuevamente."
                tipoMensaje = AppStatusMessageType.Error
            }
        }
        onDispose(detenerObservacion)
    }

    LaunchedEffect(supervision.id) {
        supervisionRepository.tieneChecklistGuardado(supervision.id) { result ->
            result.onSuccess { guardado ->
                checklistVerificado = true
                checklistGuardado = guardado
                if (guardado) {
                    mensaje = "El checklist ya fue guardado."
                    tipoMensaje = AppStatusMessageType.Info
                    supervisionRepository.obtenerDetalleHistorial(supervision.id) { detailResult ->
                        isCheckingChecklist = false
                        detailResult.onSuccess { detail ->
                            detail.checklist?.let { checklist ->
                                mapOf(
                                    "limpiezaConservacionCabina" to checklist.limpiezaConservacionCabina,
                                    "iluminacionCabina" to checklist.iluminacionCabina,
                                    "botonesPanelControl" to checklist.botonesPanelControl,
                                    "aperturaCierrePuertas" to checklist.aperturaCierrePuertas,
                                    "deteccionObstaculosPuertas" to checklist.deteccionObstaculosPuertas,
                                    "nivelacionPisos" to checklist.nivelacionPisos,
                                    "ruidosVibraciones" to checklist.ruidosVibraciones,
                                    "indicadoresPisoDireccion" to checklist.indicadoresPisoDireccion,
                                    "botonesLlamadaPisos" to checklist.botonesLlamadaPisos,
                                    "alarmaComunicacionEmergencia" to checklist.alarmaComunicacionEmergencia,
                                    "senalizacionSeguridad" to checklist.senalizacionSeguridad,
                                    "accesosLibresObstaculos" to checklist.accesosLibresObstaculos
                                ).forEach { (campo, valor) ->
                                    when (valor) {
                                        "Cumple" -> respuestas[campo] = true
                                        "No cumple" -> respuestas[campo] = false
                                    }
                                }
                                observacionesIncidencias.putAll(checklist.observacionesIncidencias)
                                observaciones = checklist.observaciones
                            }
                        }
                        detailResult.onFailure {
                            mensaje = "El checklist está guardado, pero no se pudo cargar su detalle."
                            tipoMensaje = AppStatusMessageType.Error
                        }
                    }
                } else {
                    isCheckingChecklist = false
                }
            }
            result.onFailure {
                isCheckingChecklist = false
                mensaje = "No se pudo verificar el checklist guardado. Inténtalo nuevamente."
                tipoMensaje = AppStatusMessageType.Error
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChecklistBackground)
            .safeDrawingPadding()
    ) {
        ChecklistHeader(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SupervisionHeader(
                codigo = supervision.id,
                cliente = supervision.cliente
            )

            ProgressSection(
                completadas = verificacionesCompletadas,
                total = verificaciones.size,
                progreso = progreso
            )

            Text(
                text = "Verificaciones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ChecklistInstitutionalBlue
            )
            verificaciones.forEachIndexed { index, verificacion ->
                if (index == 0 || verificaciones[index - 1].categoria != verificacion.categoria) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ChecklistLightBlue,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = verificacion.categoria,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ChecklistInstitutionalBlue
                        )
                    }
                }
                ChecklistItem(
                    numero = index + 1,
                    texto = verificacion.texto,
                    respuesta = respuestas[verificacion.campo],
                    onRespuestaChange = { cumple ->
                        respuestas[verificacion.campo] = cumple
                        if (cumple) observacionesIncidencias.remove(verificacion.campo)
                        mensaje = null
                    },
                    observacionIncidencia = observacionesIncidencias[verificacion.campo].orEmpty(),
                    onObservacionChange = { observacion ->
                        observacionesIncidencias[verificacion.campo] = observacion
                        mensaje = null
                    },
                    enabled = checklistVerificado && !isCheckingChecklist && !checklistGuardado
                )
            }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Notes, null, Modifier.size(22.dp), ChecklistInstitutionalBlue)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = "Observaciones generales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ChecklistText
                        )
                    }
                    Text(
                        text = "Registra información adicional de la supervisión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChecklistSecondaryText
                    )
                    OutlinedTextField(
                        value = observaciones,
                        onValueChange = { observaciones = it },
                        placeholder = { Text("Escribe tus observaciones aquí") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        singleLine = false,
                        enabled = checklistVerificado && !isCheckingChecklist && !checklistGuardado,
                        shape = RoundedCornerShape(12.dp),
                        colors = checklistTextFieldColors()
                    )
                }
            }

            if (!checklistGuardado) {
                Button(
                    onClick = {
                        if (!verificaciones.all { respuestas.containsKey(it.campo) }) {
                            mensaje = "Debes completar todas las verificaciones."
                            tipoMensaje = AppStatusMessageType.Warning
                            return@Button
                        }

                        val incidenciasSinObservacion = verificaciones.any { verificacion ->
                            respuestas[verificacion.campo] == false &&
                                observacionesIncidencias[verificacion.campo].isNullOrBlank()
                        }
                        if (incidenciasSinObservacion) {
                            mensaje = "Debes registrar una observación para cada respuesta No cumple."
                            tipoMensaje = AppStatusMessageType.Warning
                            return@Button
                        }

                        isSaving = true
                        mensaje = null
                        val respuestasParaFirestore = verificaciones.associate { verificacion ->
                            verificacion.campo to if (respuestas[verificacion.campo] == true) "Cumple" else "No cumple"
                        }
                        supervisionRepository.guardarChecklist(
                            supervisionId = supervision.id,
                            respuestas = respuestasParaFirestore,
                            observacionesIncidencias = observacionesIncidencias
                                .filterKeys { respuestas[it] == false }
                                .mapValues { it.value.trim() },
                            observaciones = observaciones
                        ) { result ->
                            isSaving = false
                            result.onSuccess {
                                checklistGuardado = true
                                mensaje = "Checklist guardado correctamente."
                                tipoMensaje = AppStatusMessageType.Success
                            }
                            result.onFailure {
                                mensaje = "No se pudo guardar el checklist. Inténtalo nuevamente."
                                tipoMensaje = AppStatusMessageType.Error
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChecklistInstitutionalBlue,
                        contentColor = Color.White
                    ),
                    enabled = checklistVerificado && !isCheckingChecklist && !isSaving
                ) {
                    Icon(Icons.Filled.Save, null, Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isSaving) "GUARDANDO CHECKLIST..." else "GUARDAR CHECKLIST",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (checklistGuardado && supervision.estado == "En supervision") {
                OutlinedButton(
                    onClick = { onMovilidadClick(supervision) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, ChecklistInstitutionalBlue),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = ChecklistInstitutionalBlue
                    )
                ) {
                    Icon(Icons.Filled.Navigation, null, Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("REGISTRAR MOVILIDAD", fontWeight = FontWeight.Bold)
                }
            }

            if (supervision.estado == "En supervision") {
                Button(
                    onClick = {
                        if (finalizacionHabilitada) {
                            showFinalizationDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChecklistBrightBlue,
                        contentColor = Color.White
                    ),
                    enabled = finalizacionHabilitada
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isFinalizing) "FINALIZANDO..." else "FINALIZAR SUPERVISIÓN",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                mensajeRequisitoPendiente(
                    requisitosVerificados = requisitosVerificados,
                    checklistGuardado = checklistGuardado,
                    movilidadGuardada = movilidadGuardada
                )?.let { requisitoPendiente ->
                    Text(
                        text = requisitoPendiente,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChecklistSecondaryText
                    )
                }
            }

            if (mensaje != null) {
                MensajeEstadoAplicacion(
                    type = tipoMensaje,
                    title = checklistMessageTitle(tipoMensaje),
                    message = mensaje.orEmpty()
                )
            }
        }
    }

    if (showFinalizationDialog) {
        AlertDialog(
            onDismissRequest = { if (!isFinalizing) showFinalizationDialog = false },
            title = { Text("Finalizar supervisión") },
            text = {
                Text(
                    "Se verificará en línea que el checklist y la movilidad estén registrados. Una vez finalizada, podrás consultar los resultados desde el historial."
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinalizationDialog = false },
                    enabled = !isFinalizing
                ) { Text("CANCELAR") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isFinalizing) {
                            isFinalizing = true
                            mensaje = null
                            supervisionRepository.finalizarSupervision(supervision.id) { result ->
                                isFinalizing = false
                                showFinalizationDialog = false
                                result.onSuccess {
                                    mensaje = "Supervisión finalizada correctamente."
                                    tipoMensaje = AppStatusMessageType.Success
                                    onSupervisionFinalized(supervision.copy(estado = "Finalizada"))
                                }
                                result.onFailure { error ->
                                    when {
                                        error.hasCause<MovilidadRequeridaException>() -> {
                                            mensaje = "Debes registrar una movilidad válida antes de finalizar. Usa REGISTRAR MOVILIDAD."
                                            tipoMensaje = AppStatusMessageType.Warning
                                        }
                                        error.hasCause<ChecklistRequeridoException>() -> {
                                            mensaje = "El checklist no está guardado en Firestore. Guárdalo antes de finalizar."
                                            tipoMensaje = AppStatusMessageType.Warning
                                        }
                                        else -> {
                                            mensaje = "No se pudo finalizar la supervisión. Inténtalo nuevamente."
                                            tipoMensaje = AppStatusMessageType.Error
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isFinalizing
                ) { Text("FINALIZAR") }
            }
        )
    }
}

private fun mensajeRequisitoPendiente(
    requisitosVerificados: Boolean,
    checklistGuardado: Boolean,
    movilidadGuardada: Boolean
): String? = when {
    !requisitosVerificados -> "Verificando checklist y movilidad..."
    !checklistGuardado && !movilidadGuardada -> "Falta guardar el checklist y registrar la movilidad."
    !checklistGuardado -> "Falta guardar el checklist."
    !movilidadGuardada -> "Falta registrar la movilidad."
    else -> null
}

private fun checklistMessageTitle(type: AppStatusMessageType): String = when (type) {
    AppStatusMessageType.Success -> "Operación completada"
    AppStatusMessageType.Error -> "No se pudo completar"
    AppStatusMessageType.Warning -> "Requisitos pendientes"
    AppStatusMessageType.Info -> "Información del checklist"
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) return true
        current = current.cause
    }
    return false
}

@Composable
private fun ChecklistHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .background(ChecklistInstitutionalBlue)
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
                text = "Checklist de supervisión",
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
            drawPath(backWave, ChecklistBrightBlue.copy(alpha = .48f))
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
            drawPath(frontWave, ChecklistBackground)
        }
    }
}

@Composable
private fun ChecklistItem(
    numero: Int,
    texto: String,
    respuesta: Boolean?,
    onRespuestaChange: (Boolean) -> Unit,
    observacionIncidencia: String,
    onObservacionChange: (String) -> Unit,
    enabled: Boolean
) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ChecklistLightBlue
                ) {
                    Text(
                        text = numero.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ChecklistInstitutionalBlue
                    )
                }
                Text(
                    text = texto,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ChecklistText
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnswerOption(
                    label = "Cumple",
                    selected = respuesta == true,
                    enabled = enabled,
                    onClick = { onRespuestaChange(true) },
                    modifier = Modifier.weight(1f)
                )
                AnswerOption(
                    label = "No cumple",
                    selected = respuesta == false,
                    enabled = enabled,
                    onClick = { onRespuestaChange(false) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (respuesta == false) {
                OutlinedTextField(
                    value = observacionIncidencia,
                    onValueChange = onObservacionChange,
                    label = { Text("Observación de la incidencia") },
                    supportingText = { Text("Describe el problema detectado") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = enabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = checklistTextFieldColors()
                )
            }
        }
    }
}

@Composable
private fun SupervisionHeader(codigo: String, cliente: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeaderField(Icons.AutoMirrored.Filled.Assignment, "Código de supervisión", codigo)
            HeaderField(Icons.Filled.Person, "Cliente", cliente)
        }
    }
}

@Composable
private fun HeaderField(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(22.dp), ChecklistInstitutionalBlue)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = ChecklistSecondaryText)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ChecklistText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressSection(completadas: Int, total: Int, progreso: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, null, Modifier.size(20.dp), ChecklistInstitutionalBlue)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Progreso del checklist",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ChecklistText
                )
                Text(
                    text = "${(progreso * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ChecklistInstitutionalBlue
                )
            }
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = ChecklistBrightBlue,
                trackColor = ChecklistLightBlue,
                drawStopIndicator = {}
            )
            Text(
                text = "$completadas de $total verificaciones completadas",
                style = MaterialTheme.typography.bodySmall,
                color = ChecklistSecondaryText
            )
        }
    }
}

@Composable
private fun AnswerOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedForeground = if (label == "Cumple") Color(0xFF166534) else Color(0xFF991B1B)
    val selectedBackground = if (label == "Cumple") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val containerColor = if (selected) selectedBackground else Color(0xFFF8FAFC)
    val borderColor = if (selected) selectedForeground.copy(alpha = .55f) else Color(0xFFCBD5E1)
    val contentColor = if (selected) selectedForeground else ChecklistText

    Row(
        modifier = modifier
            .height(52.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(containerColor, RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = selectedForeground,
                unselectedColor = Color(0xFF64748B),
                disabledSelectedColor = selectedForeground.copy(alpha = .5f),
                disabledUnselectedColor = Color(0xFF94A3B8)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun checklistTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = ChecklistBrightBlue,
    unfocusedBorderColor = Color(0xFFAFC7E2),
    disabledBorderColor = Color(0xFFD4DFEB),
    focusedLabelColor = ChecklistInstitutionalBlue,
    cursorColor = ChecklistBrightBlue,
    focusedTextColor = ChecklistText,
    unfocusedTextColor = ChecklistText
)

@Preview(showBackground = true)
@Composable
private fun ChecklistScreenPreview() {
    EmbarbaRutasTheme {
        PantallaListaVerificacion(
            supervision = Supervision("SUP-000", "Cliente de prueba", "Dirección", "Distrito", "00:00", "En supervision", 0.0, 0.0),
            supervisionRepository = RepositorioSupervisiones(),
            onBackClick = {}
        )
    }
}
