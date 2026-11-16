package com.embarba.rutas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.embarba.rutas.navegacion.NavegacionAplicacion
import com.embarba.rutas.interfaz.tema.EmbarbaRutasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmbarbaRutasTheme {
                NavegacionAplicacion()
            }
        }
    }
}
