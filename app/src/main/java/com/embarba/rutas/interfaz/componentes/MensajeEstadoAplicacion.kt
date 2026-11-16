package com.embarba.rutas.interfaz.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppStatusMessageType { Success, Error, Warning, Info }

@Composable
fun MensajeEstadoAplicacion(
    type: AppStatusMessageType,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = statusColors(type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.container)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = statusIcon(type),
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.content)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.content)
        }
    }
}

@Composable
private fun statusColors(type: AppStatusMessageType): StatusColors = when (type) {
    AppStatusMessageType.Success -> StatusColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    AppStatusMessageType.Error -> StatusColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    AppStatusMessageType.Warning -> StatusColors(Color(0xFFFFF3D6), Color(0xFF6B4700))
    AppStatusMessageType.Info -> StatusColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun statusIcon(type: AppStatusMessageType): ImageVector = when (type) {
    AppStatusMessageType.Success -> Icons.Filled.CheckCircle
    AppStatusMessageType.Error -> Icons.Filled.ErrorOutline
    AppStatusMessageType.Warning -> Icons.Filled.WarningAmber
    AppStatusMessageType.Info -> Icons.Filled.Info
}

private data class StatusColors(val container: Color, val content: Color)
