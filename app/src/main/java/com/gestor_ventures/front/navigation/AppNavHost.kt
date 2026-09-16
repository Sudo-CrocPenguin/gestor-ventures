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
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gestor_ventures.R
import com.gestor_ventures.front.model.TipoRegistroVentaUi
import com.gestor_ventures.front.ui.finanzas.RegistrarVentaRoute
import com.gestor_ventures.front.ui.inicio.InicioRoute
import com.gestor_ventures.front.ui.negocio.BaseFinancieraRoute
import com.gestor_ventures.front.ui.negocio.GastosFijosRoute
import com.gestor_ventures.front.ui.negocio.NegocioListoRoute
import com.gestor_ventures.front.ui.negocio.RegistrarNegocioRoute

/**
 * Rutas que no son pestañas: pantallas de flujo que se abren encima y se cierran al volver.
 */
object Rutas {
    const val RegistrarVenta = "registrar_venta"
    const val RegistrarNegocio = "registrar_negocio"

    /** HU-06. Configuración de gastos fijos del negocio activo, desde el menú lateral. */
    const val GastosFijos = "gastos_fijos"

    /** Paso 2 del onboarding; necesita saber a qué negocio configurarle la base financiera. */
    const val BaseFinanciera = "base_financiera/{$ArgumentoNegocioId}"
    fun baseFinanciera(negocioId: String) = "base_financiera/$negocioId"

    /** Cierre del onboarding con el resumen del negocio. */
    const val NegocioListo = "negocio_listo/{$ArgumentoNegocioId}"
    fun negocioListo(negocioId: String) = "negocio_listo/$negocioId"
}

/** Nombre del id de negocio dentro de la ruta. */
const val ArgumentoNegocioId = "negocioId"

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

        composable(Rutas.GastosFijos) {
            GastosFijosRoute(onBack = { navController.popBackStack() })
        }

        composable(Rutas.RegistrarNegocio) {
            RegistrarNegocioRoute(
                puedeVolver = tieneNegocios,
                onBack = { navController.popBackStack() },
                onNegocioCreado = { negocioId ->
                    onNegocioCreado(negocioId.toString())
                    // El negocio ya existe: sigue el paso 2, su base financiera.
                    navController.navigate(Rutas.baseFinanciera(negocioId.toString()))
                },
            )
        }

        composable(
            route = Rutas.BaseFinanciera,
            arguments = listOf(navArgument(ArgumentoNegocioId) { type = NavType.StringType }),
        ) { entrada ->
            val negocioId = entrada.arguments?.getString(ArgumentoNegocioId).orEmpty()
            BaseFinancieraRoute(
                onBack = { navController.popBackStack() },
                onConfiguracionLista = { navController.navigate(Rutas.negocioListo(negocioId)) },
            )
        }

        composable(
            route = Rutas.NegocioListo,
            arguments = listOf(navArgument(ArgumentoNegocioId) { type = NavType.StringType }),
        ) {
            NegocioListoRoute(
                onIrAlNegocio = {
                    // Cierra todo el onboarding: no se vuelve atrás desde el inicio.
                    navController.navigate(TopLevelDestination.Inicio.route) {
                        popUpTo(TopLevelDestination.Inicio.route) { inclusive = true }
                    }
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
