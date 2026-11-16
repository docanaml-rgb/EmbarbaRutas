package com.embarba.rutas.interfaz.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.embarba.rutas.R
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.datos.RepositorioUsuarios
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.modelos.Usuario
import com.embarba.rutas.interfaz.componentes.MensajeEstadoAplicacion
import com.embarba.rutas.interfaz.componentes.AppStatusMessageType
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme

private val HomeBackground = Color(0xFFF6F8FC)
private val InstitutionalBlue = Color(0xFF103F78)
private val BrightBlue = Color(0xFF2675D9)
private val HomeText = Color(0xFF172B4D)
private val SecondaryText = Color(0xFF60758A)
private val CounterBackground = Color(0xFFDCEBFA)

@Composable
fun HomeRoute(
    usuarioRepository: RepositorioUsuarios,
    supervisionRepository: RepositorioSupervisiones,
    supervisionActualizada: Supervision? = null,
    onSupervisionClick: (Supervision) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var supervisiones by remember { mutableStateOf<List<Supervision>>(emptyList()) }
    var isLoadingUsuario by remember { mutableStateOf(true) }
    var isLoadingSupervisiones by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        usuarioRepository.obtenerUsuarioActual { result ->
            isLoadingUsuario = false
            result.onSuccess { usuario = it }
            result.onFailure { errorMessage = "No se pudo cargar tu información. Inténtalo nuevamente." }
        }
    }
    LaunchedEffect(Unit) {
        supervisionRepository.obtenerSupervisionesAsignadas { result ->
            isLoadingSupervisiones = false
            result.onSuccess { supervisiones = it }
            result.onFailure { errorMessage = "No se pudieron cargar tus supervisiones. Inténtalo nuevamente." }
        }
    }
    LaunchedEffect(supervisionActualizada) {
        if (supervisionActualizada != null) {
            supervisiones = supervisiones.map {
                if (it.id == supervisionActualizada.id) supervisionActualizada else it
            }
        }
    }
    PantallaPrincipal(usuario, isLoadingUsuario || isLoadingSupervisiones, errorMessage,
        supervisiones, onSupervisionClick, onHistoryClick, modifier)
}

@Composable
fun PantallaPrincipal(
    usuario: Usuario?,
    isLoading: Boolean,
    errorMessage: String?,
    supervisiones: List<Supervision>,
    onSupervisionClick: (Supervision) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> Box(modifier.fillMaxSize().background(HomeBackground), Alignment.Center) {
            CircularProgressIndicator(color = InstitutionalBlue)
        }
        errorMessage != null -> ErrorContent(errorMessage, modifier)
        usuario != null -> HomeContent(usuario, supervisiones, onSupervisionClick, onHistoryClick, modifier)
    }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(HomeBackground).padding(24.dp), Alignment.Center) {
        MensajeEstadoAplicacion(AppStatusMessageType.Error, "No se pudo cargar la información", message)
    }
}

@Composable
private fun HomeContent(
    usuario: Usuario,
    supervisiones: List<Supervision>,
    onSupervisionClick: (Supervision) -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier
) {
    Box(modifier.fillMaxSize().background(HomeBackground)) {
        BottomDecorations(Modifier.fillMaxWidth().height(210.dp).align(Alignment.BottomCenter))
        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CorporateHeader() }
            item { SupervisorGreeting(usuario.nombre, Modifier.padding(horizontal = 20.dp)) }
            item { PendingCard(supervisiones.size, Modifier.padding(horizontal = 20.dp)) }
            item { HistoryButton(onHistoryClick, Modifier.padding(horizontal = 20.dp)) }
            item { SectionTitle(Modifier.padding(horizontal = 20.dp)) }
            if (supervisiones.isEmpty()) item {
                Text("No tienes supervisiones asignadas.", color = SecondaryText,
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 20.dp))
            } else items(supervisiones, key = { it.id }) { supervision ->
                SupervisionCard(supervision, { onSupervisionClick(supervision) },
                    Modifier.padding(horizontal = 20.dp))
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun CorporateHeader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(134.dp).background(InstitutionalBlue)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(76.dp), CircleShape, color = Color.White) {
                Image(painterResource(R.drawable.logoembarba), "Logo de Embarba Elevadores",
                    Modifier.padding(5.dp), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("EMBARBA", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                Text("Rutas de Supervisión", style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = .86f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(42.dp).align(Alignment.BottomCenter)) {
            val back = Path().apply {
                moveTo(0f, size.height * .42f)
                cubicTo(size.width * .26f, 0f, size.width * .68f, size.height * .9f,
                    size.width, size.height * .34f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(back, BrightBlue.copy(alpha = .5f))
            val front = Path().apply {
                moveTo(0f, size.height * .72f)
                cubicTo(size.width * .32f, size.height * .38f, size.width * .72f, size.height,
                    size.width, size.height * .58f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(front, HomeBackground)
        }
    }
}

@Composable
private fun SupervisorGreeting(name: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).background(CounterBackground, CircleShape), Alignment.Center) {
            Icon(Icons.Filled.AccountCircle, null, Modifier.size(32.dp), InstitutionalBlue)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Hola, $name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = HomeText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Supervisor", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
        }
    }
}

@Composable
private fun PendingCard(count: Int, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = CounterBackground) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Color.White.copy(alpha = .72f), CircleShape), Alignment.Center) {
                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(25.dp), InstitutionalBlue)
            }
            Spacer(Modifier.width(16.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold, color = InstitutionalBlue)
            Spacer(Modifier.width(14.dp))
            Text("Supervisiones por atender", Modifier.weight(1f), color = HomeText,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HistoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, InstitutionalBlue),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = InstitutionalBlue),
        contentPadding = PaddingValues(horizontal = 16.dp)) {
        Icon(Icons.Filled.History, null, Modifier.size(22.dp))
        Text("HISTORIAL DE SUPERVISIONES", Modifier.weight(1f), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(16.dp))
    }
}

@Composable
private fun SectionTitle(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.Assignment, null, Modifier.size(22.dp), HomeText)
        Spacer(Modifier.width(9.dp))
        Text("Supervisiones asignadas", color = HomeText, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
    }
}

private data class StatusStyle(val background: Color, val foreground: Color)
private fun statusStyle(value: String) = when (value.trim().lowercase()) {
    "pendiente" -> StatusStyle(Color(0xFFFFF4DE), Color(0xFF985800))
    "en supervisión", "en supervision" -> StatusStyle(Color(0xFFE0F2FE), Color(0xFF075985))
    "finalizada" -> StatusStyle(Color(0xFFDCFCE7), Color(0xFF166534))
    else -> StatusStyle(CounterBackground, InstitutionalBlue)
}

@Composable
private fun SupervisionCard(supervision: Supervision, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = statusStyle(supervision.estado)
    Card(modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(18.dp),
        CardDefaults.cardColors(containerColor = Color.White), CardDefaults.cardElevation(3.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(status.foreground, Offset.Zero, Size(5.dp.toPx(), size.height))
            }
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, Modifier.size(19.dp), InstitutionalBlue)
                    Spacer(Modifier.width(7.dp))
                    Text(supervision.hora, Modifier.weight(1f), color = InstitutionalBlue,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Surface(color = status.background, contentColor = status.foreground, shape = RoundedCornerShape(50)) {
                        Text(supervision.estado, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(supervision.cliente, Modifier.weight(1f), color = HomeText,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ChevronRight, "Abrir detalle", Modifier.size(24.dp), InstitutionalBlue)
                }
                InformationRow(Icons.Filled.LocationOn, supervision.direccion, 2)
                InformationRow(Icons.Filled.Apartment, supervision.distrito, 1)
            }
        }
    }
}

@Composable
private fun InformationRow(icon: ImageVector, text: String, maxLines: Int) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(18.dp), SecondaryText)
        Spacer(Modifier.width(8.dp))
        Text(text, Modifier.weight(1f), color = SecondaryText, style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BottomDecorations(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val buildings = listOf(.01f to .44f, .13f to .32f, .26f to .52f, .39f to .37f,
            .54f to .49f, .68f to .29f, .82f to .46f, .93f to .35f)
        buildings.forEachIndexed { index, pair ->
            val left = size.width * pair.first; val top = size.height * pair.second
            drawRect(InstitutionalBlue.copy(alpha = if (index % 2 == 0) .035f else .025f),
                Offset(left, top), Size(size.width * .105f, size.height - top))
        }
        val back = Path().apply {
            moveTo(0f, size.height * .7f)
            cubicTo(size.width * .3f, size.height * .53f, size.width * .63f, size.height * .84f,
                size.width, size.height * .64f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(back, BrightBlue.copy(alpha = .09f))
        val front = Path().apply {
            moveTo(0f, size.height * .84f)
            cubicTo(size.width * .3f, size.height * .67f, size.width * .7f, size.height * .97f,
                size.width, size.height * .76f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(front, InstitutionalBlue.copy(alpha = .12f))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    EmbarbaRutasTheme {
        PantallaPrincipal(Usuario("Usuario de prueba", "correo@ejemplo.com", "Supervisor"), false, null,
            listOf(Supervision("1", "Edificio corporativo Embarba", "Av. Principal 123",
                "Miraflores", "09:30 a. m.", "Pendiente", 0.0, 0.0)))
    }
}
