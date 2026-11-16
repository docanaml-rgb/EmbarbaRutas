package com.embarba.rutas.navegacion

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.embarba.rutas.interfaz.pantallas.PantallaInicioSesion
import com.embarba.rutas.interfaz.pantallas.PantallaListaVerificacion
import com.embarba.rutas.interfaz.pantallas.PantallaDetalleSupervision
import com.embarba.rutas.datos.RepositorioAutenticacion
import com.embarba.rutas.datos.RepositorioSupervisiones
import com.embarba.rutas.datos.RepositorioUsuarios
import com.embarba.rutas.modelos.Supervision
import com.embarba.rutas.interfaz.pantallas.HomeRoute
import com.embarba.rutas.interfaz.pantallas.PantallaHistorial
import com.embarba.rutas.interfaz.pantallas.PantallaDetalleHistorial
import com.embarba.rutas.interfaz.pantallas.PantallaMovilidad

private object AppRoute {
    const val Login = "login"
    const val Home = "home"
    const val Detail = "supervision/{supervisionId}"
    const val Checklist = "checklist/{supervisionId}"
    const val Movilidad = "movilidad/{supervisionId}"
    const val History = "history"
    const val HistoryDetail = "historyDetail/{supervisionId}"

    fun detail(supervisionId: String) = "supervision/${Uri.encode(supervisionId)}"
    fun checklist(supervisionId: String) = "checklist/${Uri.encode(supervisionId)}"
    fun movilidad(supervisionId: String) = "movilidad/${Uri.encode(supervisionId)}"
    fun historyDetail(supervisionId: String) = "historyDetail/${Uri.encode(supervisionId)}"
}

@Composable
fun NavegacionAplicacion() {
    val navController = rememberNavController()
    val authRepository = remember { RepositorioAutenticacion() }
    val usuarioRepository = remember { RepositorioUsuarios() }
    val supervisionRepository = remember { RepositorioSupervisiones() }
    var supervisionSeleccionada by remember { mutableStateOf<Supervision?>(null) }
    var supervisionActualizada by remember { mutableStateOf<Supervision?>(null) }

    NavHost(navController = navController, startDestination = AppRoute.Login) {
        composable(AppRoute.Login) {
            PantallaInicioSesion(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.Login) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.Home) {
            HomeRoute(
                usuarioRepository = usuarioRepository,
                supervisionRepository = supervisionRepository,
                supervisionActualizada = supervisionActualizada,
                onSupervisionClick = { supervision ->
                    supervisionSeleccionada = supervision
                    navController.navigate(AppRoute.detail(supervision.id))
                },
                onHistoryClick = {
                    navController.navigate(AppRoute.History)
                }
            )
        }
        composable(
            route = AppRoute.Detail,
            arguments = listOf(navArgument("supervisionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val supervisionId = backStackEntry.arguments?.getString("supervisionId")
            val supervision = supervisionSeleccionada?.takeIf { it.id == supervisionId }

            if (supervision != null) {
                PantallaDetalleSupervision(
                    supervision = supervision,
                    onBackClick = { navController.popBackStack() },
                    supervisionRepository = supervisionRepository,
                    onArrivalRegistered = { supervisionConLlegada ->
                        supervisionSeleccionada = supervisionConLlegada
                        supervisionActualizada = supervisionConLlegada
                    },
                    onChecklistClick = { supervisionConChecklist ->
                        navController.navigate(AppRoute.checklist(supervisionConChecklist.id))
                    }
                )
            }
        }
        composable(
            route = AppRoute.Checklist,
            arguments = listOf(navArgument("supervisionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val supervisionId = backStackEntry.arguments?.getString("supervisionId")
            val supervision = supervisionSeleccionada?.takeIf { it.id == supervisionId }

            if (supervision != null) {
                PantallaListaVerificacion(
                    supervision = supervision,
                    supervisionRepository = supervisionRepository,
                    onBackClick = { navController.popBackStack() },
                    onMovilidadClick = { supervisionConMovilidad ->
                        navController.navigate(AppRoute.movilidad(supervisionConMovilidad.id))
                    },
                    onSupervisionFinalized = { supervisionFinalizada ->
                        supervisionSeleccionada = supervisionFinalizada
                        supervisionActualizada = supervisionFinalizada
                        navController.popBackStack(AppRoute.Home, inclusive = false)
                    }
                )
            }
        }
        composable(
            route = AppRoute.Movilidad,
            arguments = listOf(navArgument("supervisionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val supervisionId = backStackEntry.arguments?.getString("supervisionId")
            val supervision = supervisionSeleccionada?.takeIf { it.id == supervisionId }

            if (supervision != null) {
                PantallaMovilidad(
                    supervision = supervision,
                    supervisionRepository = supervisionRepository,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(AppRoute.History) {
            PantallaHistorial(
                supervisionRepository = supervisionRepository,
                onBackClick = { navController.popBackStack() },
                onSupervisionClick = { supervision ->
                    supervisionSeleccionada = supervision
                    navController.navigate(AppRoute.historyDetail(supervision.id))
                }
            )
        }
        composable(
            route = AppRoute.HistoryDetail,
            arguments = listOf(navArgument("supervisionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val supervisionId = backStackEntry.arguments?.getString("supervisionId")
            val supervision = supervisionSeleccionada?.takeIf { it.id == supervisionId }

            if (supervision != null) {
                PantallaDetalleHistorial(
                    supervision = supervision,
                    supervisionRepository = supervisionRepository,
                    usuarioRepository = usuarioRepository,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
