package com.gestor_ventures.front.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gestor_ventures.R
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.ui.finanzas.RegistrarVentaRoute
import com.gestor_ventures.front.ui.inicio.InicioRoute
import com.gestor_ventures.front.ui.negocio.RegistrarNegocioRoute

/**
 * Rutas que no son pestañas: pantallas de flujo que se abren encima y se cierran al volver.
 */
object Rutas {
    const val RegistrarVenta = "registrar_venta"
    const val RegistrarNegocio = "registrar_negocio"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    mostrarMensaje: (String) -> Unit,
    onNegocioCreado: (String) -> Unit,
    tieneNegocios: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Inicio.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Inicio.route) {
            InicioRoute(
                onVerFinanzas = { navController.navigateToTopLevel(TopLevelDestination.Finanzas) },
                onRegistrarVenta = { navController.navigate(Rutas.RegistrarVenta) },
                // Abrir caja (Épica 4) aún no tiene pantalla.
                onAbrirCaja = {},
            )
        }

        composable(Rutas.RegistrarVenta) {
            val mensajeDetallada = stringResource(R.string.registrar_venta_guardada)
            val mensajeRapida = stringResource(R.string.registrar_venta_rapida_guardada)
            RegistrarVentaRoute(
                onBack = { navController.popBackStack() },
                onVentaGuardada = { tipo ->
                    navController.popBackStack()
                    mostrarMensaje(
                        if (tipo == TipoRegistroVentaUi.Rapido) mensajeRapida else mensajeDetallada,
                    )
                },
            )
        }

        composable(Rutas.RegistrarNegocio) {
            val mensajeCreado = stringResource(R.string.negocio_creado)
            RegistrarNegocioRoute(
                puedeVolver = tieneNegocios,
                onBack = { navController.popBackStack() },
                onNegocioCreado = { negocioId ->
                    onNegocioCreado(negocioId.toString())
                    navController.popBackStack()
                    mostrarMensaje(mensajeCreado)
                },
            )
        }

        // Pestañas cuya pantalla aún no existe: se reemplazan a medida que se construyen.
        listOf(
            TopLevelDestination.Finanzas,
            TopLevelDestination.Agenda,
            TopLevelDestination.Clientes,
            TopLevelDestination.Asistente,
        ).forEach { destination ->
            composable(destination.route) { PendingDestination(destination) }
        }
    }
}

/** Cambia de pestaña conservando el estado de cada una, sin apilar pestañas repetidas. */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PendingDestination(destination: TopLevelDestination) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.pantalla_pendiente, stringResource(destination.labelRes)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
