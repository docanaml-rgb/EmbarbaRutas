package com.embarba.rutas.ubicacion

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper

class AyudanteUbicacion(context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)

    fun obtenerUbicacionActual(
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> {
                onError("Activa la ubicación del dispositivo e inténtalo nuevamente.")
                return
            }
        }

        val handler = Handler(Looper.getMainLooper())
        var completed = false
        lateinit var locationListener: LocationListener

        fun finalizar(onFinish: () -> Unit) {
            if (completed) return
            completed = true
            locationManager.removeUpdates(locationListener)
            onFinish()
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                finalizar { onLocation(location) }
            }

            override fun onProviderDisabled(provider: String) {
                finalizar { onError("La ubicación del dispositivo no está disponible.") }
            }
        }

        try {
            locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper())
            handler.postDelayed({
                finalizar { onError("No se pudo obtener tu ubicación. Inténtalo nuevamente.") }
            }, LOCATION_TIMEOUT_MS)
        } catch (_: SecurityException) {
            onError("No se concedió el permiso de ubicación.")
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 10_000L
    }
}
