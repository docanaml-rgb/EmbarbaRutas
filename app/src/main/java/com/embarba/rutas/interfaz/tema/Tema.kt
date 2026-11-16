package com.embarba.rutas.interfaz.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EmbarbaLightColorScheme = lightColorScheme(
    primary = EmbarbaBlue,
    onPrimary = EmbarbaSurface,
    primaryContainer = EmbarbaBlueLight,
    onPrimaryContainer = EmbarbaBlueDark,
    secondary = EmbarbaRed,
    onSecondary = EmbarbaSurface,
    secondaryContainer = EmbarbaRedLight,
    onSecondaryContainer = EmbarbaText,
    background = EmbarbaBackground,
    onBackground = EmbarbaText,
    surface = EmbarbaSurface,
    onSurface = EmbarbaText,
    surfaceVariant = EmbarbaSurfaceSoft,
    onSurfaceVariant = EmbarbaTextMuted,
    outline = EmbarbaTextMuted,
    error = EmbarbaRed
)

@Composable
fun EmbarbaRutasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmbarbaLightColorScheme,
        typography = Typography,
        content = content
    )
}
