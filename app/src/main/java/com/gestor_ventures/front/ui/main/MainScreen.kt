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
import com.gestor_ventures.front.components.AnchoMaximoApp
import com.gestor_ventures.front.components.GvAnchoDeLectura
import com.gestor_ventures.front.navigation.GvBottomBar
import com.gestor_ventures.front.navigation.Rutas
import com.gestor_ventures.front.navigation.navigateToTopLevel
import com.gestor_ventures.front.ui.menu.MenuLateral
import com.gestor_ventures.front.ui.menu.OpcionMenu
import com.gestor_ventures.front.ui.tour.EscenaTour
import com.gestor_ventures.front.ui.tour.TourScreen
import com.gestor_ventures.front.ui.tour.TourUiState
import com.gestor_ventures.front.ui.tour.TourViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import com.gestor_ventures.front.components.LocalTourRegistro
import com.gestor_ventures.front.components.TourRegistro
import com.gestor_ventures.front.navigation.TopLevelDestination
import com.gestor_ventures.back.model.ModoTema
import com.gestor_ventures.front.ui.menu.AparienciaDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gestor_ventures.front.model.RolNegocio
import com.gestor_ventures.front.navigation.destinosDe

@Composable
fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
    tourViewModel: TourViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tourState by tourViewModel.uiState.collectAsStateWithLifecycle()
    MainScreen(
        uiState = uiState,
        onNegocioSeleccionado = viewModel::seleccionarNegocio,
        onCerrarSesion = viewModel::cerrarSesion,
        onVerTour = { tourViewModel.reiniciar(esLider = uiState.negocioActivo?.rol != RolNegocio.Vendedor) },
        onModoTemaChange = viewModel::cambiarModoTema,
        tourState = tourState,
        onTourSiguiente = tourViewModel::siguiente,
        onTourAnterior = tourViewModel::anterior,
        onTourCerrar = tourViewModel::cerrar,
        onTourListo = {
            tourViewModel.mostrarSiEsLaPrimeraVez(esLider = uiState.negocioActivo?.rol != RolNegocio.Vendedor)
        },
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
    onCerrarSesion: () -> Unit,
    onVerTour: () -> Unit = {},
    onModoTemaChange: (ModoTema) -> Unit = {},
    tourState: TourUiState = TourUiState(),
    onTourSiguiente: () -> Unit = {},
    onTourAnterior: () -> Unit = {},
    onTourCerrar: () -> Unit = {},
    /** Avisa que ya hay negocio y pestañas a la vista: es el momento de abrir el tour. */
    onTourListo: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Las pantallas de flujo (registrar venta, abrir caja…) traen su propia barra y ocupan
    // todo el alto: el marco con pestañas solo acompaña a las pestañas.
    val esLider = uiState.negocioActivo?.rol != RolNegocio.Vendedor
    val destinos = destinosDe(esLider)
    val enPestana = TopLevelDestination.entries.any { it.route == currentRoute }

    // Al cambiar a un negocio donde solo es vendedor, una pestaña que ya no le corresponde
    // (Finanzas, Asistente) lo deja en el inicio en vez de mostrarle lo que no debe ver.
    LaunchedEffect(esLider, currentRoute) {
        if (enPestana && destinos.none { it.route == currentRoute }) {
            navController.navigateToTopLevel(TopLevelDestination.Inicio)
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mostrarApariencia by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    /** Aviso temporal mientras la pantalla de esa opción no existe. */
    fun avisarPendiente(etiqueta: String) {
        scope.launch {
            drawerState.close()
            snackbarHostState.showSnackbar(context.getString(R.string.pantalla_pendiente, etiqueta))
        }
    }

    /** Las opciones que ya tienen pantalla navegan; las demás siguen avisando que faltan. */
    fun abrirOpcion(opcion: OpcionMenu) {
        // Cerrar sesión no es una pantalla: se cierra el menú y AppRoot cambia al login solo.
        if (opcion == OpcionMenu.CerrarSesion) {
            scope.launch { drawerState.close() }
            onCerrarSesion()
            return
        }
        if (opcion == OpcionMenu.Apariencia) {
            scope.launch { drawerState.close() }
            mostrarApariencia = true
            return
        }
        if (opcion == OpcionMenu.VerTour) {
            scope.launch { drawerState.close() }
            onVerTour()
            return
        }

        val ruta = when (opcion) {
            OpcionMenu.InformacionNegocio -> Rutas.EditarNegocio
            OpcionMenu.MiPerfil -> Rutas.Perfil
            OpcionMenu.Notificaciones -> Rutas.Notificaciones
            OpcionMenu.GastosFijos -> Rutas.GastosFijos
            OpcionMenu.MetaYReinversion -> Rutas.MetaYReinversion
            OpcionMenu.Categorias -> Rutas.Categorias
            OpcionMenu.MetodosPago -> Rutas.MetodosPago
            OpcionMenu.WhatsAppEmpresa -> Rutas.WhatsAppEmpresa
            OpcionMenu.Equipo -> Rutas.Equipo
            else -> null
        }
        if (ruta == null) {
            avisarPendiente(context.getString(opcion.labelRes))
            return
        }
        scope.launch { drawerState.close() }
        navController.navigate(ruta)
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

    // HU-05: una cuenta nueva sin negocios no tiene nada que mostrar, así que se abre el registro de
    // negocio. Una cuenta que ya existía no lo necesita: ve el inicio con el botón para registrar uno.
    LaunchedEffect(uiState.debeRegistrarNegocio, currentRoute) {
        if (uiState.debeRegistrarNegocio && currentRoute != Rutas.RegistrarNegocio) {
            navController.navigate(Rutas.RegistrarNegocio)
        }
    }

    // El tour espera a que se vean las pestañas con un negocio ya creado: durante el onboarding
    // todavía no hay nada que explicar.
    val tourListo = enPestana && uiState.negocios.isNotEmpty()
    LaunchedEffect(tourListo) {
        if (tourListo) onTourListo()
    }

    // El tour necesita saber dónde está cada botón (lo anotan las pantallas con `tourTarget`) y
    // llevar la app a la escena de cada paso: cambiar de pestaña o abrir el menú lateral.
    val tourRegistro = remember { TourRegistro() }
    LaunchedEffect(tourState.visible, tourState.paso) {
        if (!tourState.visible) {
            drawerState.close()
            return@LaunchedEffect
        }
        when (val escena = tourState.actual.escena) {
            EscenaTour.Menu -> drawerState.open()
            else -> {
                drawerState.close()
                val destino = when (escena) {
                    EscenaTour.Finanzas -> TopLevelDestination.Finanzas
                    EscenaTour.Agenda -> TopLevelDestination.Agenda
                    EscenaTour.Clientes -> TopLevelDestination.Clientes
                    EscenaTour.Asistente -> TopLevelDestination.Asistente
                    else -> TopLevelDestination.Inicio
                }
                if (currentRoute != destino.route) navController.navigateToTopLevel(destino)
            }
        }
    }

    CompositionLocalProvider(LocalTourRegistro provides tourRegistro) {
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
                onOpcionClick = { opcion: OpcionMenu -> abrirOpcion(opcion) },
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
                        onNotificacionesClick = { navController.navigate(Rutas.Notificaciones) },
                        modifier = Modifier.statusBarsPadding(),
                    )
                }
            },
            bottomBar = {
                if (enPestana) {
                    GvBottomBar(
                        destinations = destinos,
                        currentRoute = currentRoute,
                        onDestinationClick = navController::navigateToTopLevel,
                    )
                }
            },
        ) { innerPadding ->
            // Las barras van de borde a borde; el contenido se queda en su columna, con el
            // tope holgado: en una tablet acostada, 600 dp dejaba dos franjas muertas a los
            // lados y sin tope las tarjetas se estiran de punta a punta.
            GvAnchoDeLectura(Modifier.padding(innerPadding), maximo = AnchoMaximoApp) {
                AppNavHost(
                    navController = navController,
                    mostrarMensaje = { mensaje -> scope.launch { snackbarHostState.showSnackbar(mensaje) } },
                    onNegocioCreado = onNegocioSeleccionado,
                    tieneNegocios = uiState.negocios.isNotEmpty(),
                )
            }
        }
    }

    if (mostrarApariencia) {
        AparienciaDialog(
            modo = uiState.modoTema,
            onElegir = onModoTemaChange,
            onCerrar = { mostrarApariencia = false },
        )
    }

    TourScreen(
        uiState = tourState,
        registro = tourRegistro,
        onSiguiente = onTourSiguiente,
        onAnterior = onTourAnterior,
        onCerrar = onTourCerrar,
    )
    }
}
