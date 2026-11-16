package com.embarba.rutas.interfaz.pantallas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.embarba.rutas.R
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.ubicacion.AyudanteUbicacion
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.interfaz.componentes.MensajeEstadoAplicacion
import com.embarba.rutas.interfaz.componentes.AppStatusMessageType
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme

private val DetailBackground = Color(0xFFF6F8FC)
private val DetailInstitutionalBlue = Color(0xFF103F78)
private val DetailBrightBlue = Color(0xFF2675D9)
private val DetailText = Color(0xFF172B4D)
private val DetailSecondaryText = Color(0xFF60758A)

@Composable
fun PantallaDetalleSupervision(
    supervision: Supervision,
    onBackClick: () -> Unit,
    supervisionRepository: RepositorioSupervisiones,
    onArrivalRegistered: (Supervision) -> Unit = {},
    onChecklistClick: (Supervision) -> Unit = {},
    onViewLocationClick: (Supervision) -> Unit = {},
    onStartRouteClick: (Supervision) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationHelper = remember { AyudanteUbicacion(context.applicationContext) }
    val isArrivalRegistered = supervision.estado == "En supervision"
    var isValidatingLocation by remember { mutableStateOf(false) }
    var isSavingArrival by remember { mutableStateOf(false) }
    var arrivalMessage by remember(supervision.id, supervision.estado) {
        mutableStateOf(if (isArrivalRegistered) "Llegada registrada correctamente." else null)
    }
    var arrivalMessageType by remember(supervision.id, supervision.estado) {
        mutableStateOf(
            if (isArrivalRegistered) AppStatusMessageType.Success else AppStatusMessageType.Info
        )
    }

    val validarLlegada: () -> Unit = {
        if (supervision.estado == "En supervision") {
            arrivalMessage = "La llegada ya fue registrada para esta supervisión."
            arrivalMessageType = AppStatusMessageType.Info
        } else if (!tieneCoordenadasValidas(supervision)) {
            arrivalMessage = "La supervisión no tiene coordenadas válidas."
            arrivalMessageType = AppStatusMessageType.Error
        } else {
            isValidatingLocation = true
            arrivalMessage = null
            locationHelper.obtenerUbicacionActual(
                onLocation = { ubicacionActual ->
                    isValidatingLocation = false
                    val distanciaMetros = calcularDistancia(ubicacionActual, supervision)
                    if (distanciaMetros <= ARRIVAL_RADIUS_METERS) {
                        isSavingArrival = true
                        supervisionRepository.registrarLlegada(
                            supervisionId = supervision.id,
                            latitudLlegada = ubicacionActual.latitude,
                            longitudLlegada = ubicacionActual.longitude
                        ) { result ->
                            isSavingArrival = false
                            result.onSuccess {
                                onArrivalRegistered(supervision.copy(estado = "En supervision"))
                                arrivalMessage = "Llegada registrada correctamente."
                                arrivalMessageType = AppStatusMessageType.Success
                            }
                            result.onFailure {
                                arrivalMessage = "No se pudo registrar la llegada. Inténtalo nuevamente."
                                arrivalMessageType = AppStatusMessageType.Error
                            }
                        }
                    } else {
                        arrivalMessage = mensajeDeDistancia(distanciaMetros)
                        arrivalMessageType = AppStatusMessageType.Warning
                    }
                },
                onError = { mensaje ->
                    isValidatingLocation = false
                    arrivalMessage = mensaje
                    arrivalMessageType = AppStatusMessageType.Error
                }
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        if (permisos.values.any { it }) {
            validarLlegada()
        } else {
            arrivalMessage = "Se necesita el permiso de ubicación para registrar la llegada."
            arrivalMessageType = AppStatusMessageType.Warning
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DetailBackground)
            .safeDrawingPadding()
    ) {
        DetailHeader(onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailCard(supervision)

            DestinationSection(supervision.direccion)

            OutlinedButton(
                onClick = {
                    onViewLocationClick(supervision)
                    abrirUbicacion(context, supervision)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, DetailInstitutionalBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = DetailInstitutionalBlue
                ),
                enabled = !isArrivalRegistered
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                Text("VER UBICACIÓN", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    onStartRouteClick(supervision)
                    iniciarRuta(context, supervision)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DetailBrightBlue,
                    contentColor = Color.White
                ),
                enabled = !isArrivalRegistered
            ) {
                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                Text("INICIAR RUTA", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (tienePermisoDeUbicacion(context)) {
                        validarLlegada()
                    } else {
                        permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DetailInstitutionalBlue,
                    contentColor = Color.White
                ),
                enabled = !isValidatingLocation && !isSavingArrival && supervision.estado == "Pendiente"
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isValidatingLocation) "OBTENIENDO UBICACIÓN..." else "REGISTRAR LLEGADA",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (arrivalMessage != null) {
                MensajeEstadoAplicacion(
                    type = arrivalMessageType,
                    title = arrivalMessageTitle(arrivalMessageType),
                    message = arrivalMessage.orEmpty()
                )
            }
            if (supervision.estado == "En supervision") {
                Button(
                    onClick = { onChecklistClick(supervision) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DetailInstitutionalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, null, Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("REALIZAR CHECKLIST", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun arrivalMessageTitle(type: AppStatusMessageType): String = when (type) {
    AppStatusMessageType.Success -> "Llegada registrada"
    AppStatusMessageType.Error -> "No se pudo continuar"
    AppStatusMessageType.Warning -> "Atención"
    AppStatusMessageType.Info -> "Información de llegada"
}

private fun tienePermisoDeUbicacion(context: Context): Boolean {
    return LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun tieneCoordenadasValidas(supervision: Supervision): Boolean {
    return supervision.latitud in -90.0..90.0 &&
        supervision.longitud in -180.0..180.0 &&
        !(supervision.latitud == 0.0 && supervision.longitud == 0.0)
}

private fun calcularDistancia(ubicacionActual: Location, supervision: Supervision): Float {
    val resultado = FloatArray(1)
    Location.distanceBetween(
        ubicacionActual.latitude,
        ubicacionActual.longitude,
        supervision.latitud,
        supervision.longitud,
        resultado
    )
    return resultado[0]
}

private fun mensajeDeDistancia(distanciaMetros: Float): String {
    return "No puedes registrar la llegada. Te encuentras a %.2f km del destino."
        .format(distanciaMetros / 1_000)
}

private fun mensajeDeProximidad(ubicacionActual: Location, supervision: Supervision): String {
    val resultado = FloatArray(1)
    Location.distanceBetween(
        ubicacionActual.latitude,
        ubicacionActual.longitude,
        supervision.latitud,
        supervision.longitud,
        resultado
    )
    val distanciaMetros = resultado[0]

    return if (distanciaMetros <= ARRIVAL_RADIUS_METERS) {
        "Ubicación validada. Te encuentras a ${distanciaMetros.toInt()} m del destino."
    } else {
        "No puedes registrar la llegada. Te encuentras a %.2f km del destino."
            .format(distanciaMetros / 1_000)
    }
}

private fun abrirUbicacion(context: Context, supervision: Supervision) {
    val coordenadas = "${supervision.latitud},${supervision.longitud}"
    val uri = Uri.parse("geo:$coordenadas?q=$coordenadas")
    val googleMapsIntent = Intent(Intent.ACTION_VIEW, uri)
        .setPackage("com.google.android.apps.maps")
    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)

    abrirConFallback(context, googleMapsIntent, fallbackIntent)
}

private fun iniciarRuta(context: Context, supervision: Supervision) {
    if (!tieneCoordenadasValidas(supervision)) {
        Toast.makeText(context, "La supervisión no tiene coordenadas válidas.", Toast.LENGTH_SHORT).show()
        return
    }

    val coordenadas = "${supervision.latitud},${supervision.longitud}"
    val googleMapsIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("google.navigation:q=$coordenadas&mode=d")
    ).setPackage("com.google.android.apps.maps")
    val fallbackIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                "&destination=${Uri.encode(coordenadas)}" +
                "&travelmode=driving"
        )
    )

    abrirConFallback(context, googleMapsIntent, fallbackIntent)
}

private fun abrirConFallback(context: Context, googleMapsIntent: Intent, fallbackIntent: Intent) {
    try {
        context.startActivity(googleMapsIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay una aplicación de mapas disponible.", Toast.LENGTH_SHORT).show()
        }
    }
}

private const val ARRIVAL_RADIUS_METERS = 200f

private val LOCATION_PERMISSIONS = arrayOf(
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
private fun DetailHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .background(DetailInstitutionalBlue)
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
            drawPath(backWave, DetailBrightBlue.copy(alpha = .48f))

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
            drawPath(frontWave, DetailBackground)
        }
    }
}

private data class DetailStatusStyle(val background: Color, val foreground: Color)

private fun detailStatusStyle(status: String): DetailStatusStyle = when (status.trim().lowercase()) {
    "pendiente" -> DetailStatusStyle(Color(0xFFFFF4DE), Color(0xFF985800))
    "en supervisión", "en supervision" -> DetailStatusStyle(Color(0xFFE0F2FE), Color(0xFF075985))
    "finalizada" -> DetailStatusStyle(Color(0xFFDCFCE7), Color(0xFF166534))
    else -> DetailStatusStyle(Color(0xFFDCEBFA), DetailInstitutionalBlue)
}

@Composable
private fun DetailCard(supervision: Supervision) {
    val statusStyle = detailStatusStyle(supervision.estado)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = DetailInstitutionalBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Código de supervisión",
                        style = MaterialTheme.typography.labelLarge,
                        color = DetailSecondaryText
                    )
                    Text(
                        text = supervision.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DetailText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    color = statusStyle.background,
                    contentColor = statusStyle.foreground,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = supervision.estado,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFE4EAF1))
            DetailField(Icons.Filled.Person, "Cliente", supervision.cliente, 2)
            DetailField(Icons.Filled.LocationOn, "Dirección", supervision.direccion, 3)
            DetailField(Icons.Filled.Apartment, "Distrito", supervision.distrito, 2)
            DetailField(Icons.Filled.CalendarMonth, "Hora programada", supervision.hora, 1)
        }
    }
}

@Composable
private fun DetailField(icon: ImageVector, label: String, value: String, maxLines: Int) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(21.dp), DetailInstitutionalBlue)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = DetailSecondaryText)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = DetailText,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DestinationSection(address: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, Modifier.size(22.dp), DetailText)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Ubicación del destino",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DetailText
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "La ubicación estará disponible en el mapa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetailSecondaryText
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SupervisionDetailScreenPreview() {
    EmbarbaRutasTheme {
        PantallaDetalleSupervision(
            supervision = Supervision(
                id = "SUP-000",
                cliente = "Cliente de prueba",
                direccion = "Dirección de prueba",
                distrito = "Distrito de prueba",
                hora = "00:00",
                estado = "Pendiente",
                latitud = 0.0,
                longitud = 0.0
            ),
            onBackClick = {},
            supervisionRepository = RepositorioSupervisiones()
        )
    }
}
