package com.gestor_ventures.front.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gestor_ventures.R
import com.gestor_ventures.front.components.GvTopBar
import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.navigation.AppNavHost
import com.gestor_ventures.front.navigation.GvBottomBar
import com.gestor_ventures.front.navigation.Rutas
import com.gestor_ventures.front.navigation.destinosLider
import com.gestor_ventures.front.navigation.navigateToTopLevel
import com.gestor_ventures.front.ui.menu.MenuLateral
import com.gestor_ventures.front.ui.menu.OpcionMenu
import kotlinx.coroutines.launch

@Composable
fun MainRoute(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreen(
        uiState = uiState,
        onNegocioSeleccionado = viewModel::seleccionarNegocio,
    )
}

/**
 * Marco común del rol Líder: menú lateral, barra superior, negocio activo, contenido de la
 * pestaña actual y barra de pestañas.
 */
@Composable
fun MainScreen(
    uiState: MainUiState,
    onNegocioSeleccionado: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Las pantallas de flujo (registrar venta, abrir caja…) traen su propia barra y ocupan
    // todo el alto: el marco con pestañas solo acompaña a las pestañas.
    val enPestana = destinosLider.any { it.route == currentRoute }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    /** Aviso temporal mientras la pantalla de esa opción no existe. */
    fun avisarPendiente(etiqueta: String) {
        scope.launch {
            drawerState.close()
            snackbarHostState.showSnackbar(context.getString(R.string.pantalla_pendiente, etiqueta))
        }
    }

    fun cambiarNegocio(negocio: NegocioUi) {
        onNegocioSeleccionado(negocio.id)
        scope.launch {
            drawerState.close()
            snackbarHostState.showSnackbar(
                context.getString(
                    R.string.menu_negocio_cambiado,
                    negocio.nombre,
                    context.getString(negocio.rol.labelRes),
                ),
            )
        }
    }

    // HU-05: sin negocios no hay nada que mostrar, así que se abre el registro de negocio.
    LaunchedEffect(uiState.sinNegocios, currentRoute) {
        if (uiState.sinNegocios && currentRoute != Rutas.RegistrarNegocio) {
            navController.navigate(Rutas.RegistrarNegocio)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = enPestana,
        drawerContent = {
            MenuLateral(
                uiState = uiState.menu,
                onNegocioClick = { negocio -> cambiarNegocio(negocio) },
                onAgregarNegocioClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Rutas.RegistrarNegocio)
                },
                onOpcionClick = { opcion: OpcionMenu ->
                    avisarPendiente(context.getString(opcion.labelRes))
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (enPestana) {
                    // El título es el negocio activo: el menú lateral es el que lo cambia.
                    GvTopBar(
                        titulo = uiState.negocioActivo?.nombre ?: stringResource(R.string.app_name),
                        notificacionesSinLeer = uiState.notificacionesSinLeer,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        // Las notificaciones (HU-40) aún no tienen pantalla.
                        onNotificacionesClick = {
                            avisarPendiente(context.getString(R.string.menu_notificaciones))
                        },
                        modifier = Modifier.statusBarsPadding(),
                    )
                }
            },
            bottomBar = {
                if (enPestana) {
                    GvBottomBar(
                        destinations = destinosLider,
                        currentRoute = currentRoute,
                        onDestinationClick = navController::navigateToTopLevel,
                    )
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                mostrarMensaje = { mensaje -> scope.launch { snackbarHostState.showSnackbar(mensaje) } },
                onNegocioCreado = onNegocioSeleccionado,
                tieneNegocios = uiState.negocios.isNotEmpty(),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
