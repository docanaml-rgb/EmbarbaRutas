package com.embarba.rutas.interfaz.componentes

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.embarba.rutas.R

@Composable
fun BotonRegresar(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Regresar",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
