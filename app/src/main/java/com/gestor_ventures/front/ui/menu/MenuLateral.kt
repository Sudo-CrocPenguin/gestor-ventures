package com.gestor_ventures.front.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gestor_ventures.R
import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.theme.GestorVenturesTheme

/** Forma común de las filas del menú (negocios y "Agregar negocio"). */
internal val MenuItemShape = RoundedCornerShape(13.dp)

/** El mockup abre el menú sobre el 83 % del ancho de la pantalla. */
private const val AnchoMenu = 0.83f

/**
 * Tope del ancho del menú. El 83 % está pensado para un teléfono; en una tablet acostada son
 * más de 1.100 dp de menú para una lista de opciones cortas, y lo que se tapa es la app.
 */
private val AnchoMaximoMenu = 360.dp

/**
 * El 83 % de la pantalla, sin pasarse de [AnchoMaximoMenu].
 *
 * Se calcula y se aplica con `width` en vez de encadenar `fillMaxWidth(…).widthIn(max = …)`:
 * eso no recorta nada, porque `fillMaxWidth` deja el ancho fijo y `widthIn` ya no puede
 * bajarlo.
 */
@Composable
private fun anchoDelMenu(): Dp {
    val anchoPantalla = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    return minOf(anchoPantalla * AnchoMenu, AnchoMaximoMenu)
}

/** Secciones plegables del menú. */
private enum class SeccionMenu { General, Negocios, Configuracion }

/**
 * Menú lateral que abre el botón de las tres rayas. En orden: perfil del usuario, opciones
 * generales, sus negocios con el rol de cada uno, la configuración del negocio activo (solo
 * la ve el Líder) y, al pie, cerrar sesión.
 *
 * No tiene estado propio: recibe [uiState] y avisa de cada acción hacia arriba, para que el
 * negocio activo tenga una sola fuente de verdad en `MainViewModel`.
 */
@Composable
fun MenuLateral(
    uiState: MenuLateralUiState,
    onNegocioClick: (NegocioUi) -> Unit,
    onAgregarNegocioClick: () -> Unit,
    onOpcionClick: (OpcionMenu) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Qué secciones están desplegadas. Es estado visual del menú, así que vive acá y no en el
    // ViewModel; `rememberSaveable` lo conserva al girar la pantalla.
    var abiertas by rememberSaveable { mutableStateOf(SeccionMenu.entries.toSet()) }

    fun alternar(seccion: SeccionMenu) {
        abiertas = if (seccion in abiertas) abiertas - seccion else abiertas + seccion
    }

    ModalDrawerSheet(
        modifier = modifier.width(anchoDelMenu()),
        drawerShape = RectangleShape,
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        uiState.usuario?.let { CabeceraUsuario(it) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SeccionPlegable(
                titulo = stringResource(R.string.menu_general),
                expandida = SeccionMenu.General in abiertas,
                onToggle = { alternar(SeccionMenu.General) },
            ) {
                opcionesGenerales.forEach { opcion ->
                    OpcionItem(opcion = opcion, onClick = { onOpcionClick(opcion) })
                }
            }

            SeccionPlegable(
                titulo = stringResource(R.string.menu_mis_negocios),
                expandida = SeccionMenu.Negocios in abiertas,
                onToggle = { alternar(SeccionMenu.Negocios) },
            ) {
                Column(Modifier.selectableGroup()) {
                    uiState.negocios.forEach { negocio ->
                        NegocioItem(
                            negocio = negocio,
                            activo = negocio.id == uiState.negocioActivoId,
                            onClick = { onNegocioClick(negocio) },
                        )
                    }
                }
                AgregarNegocioItem(onClick = onAgregarNegocioClick)
            }

            val configuracion = uiState.opcionesConfiguracion
            if (configuracion.isNotEmpty()) {
                SeccionPlegable(
                    titulo = stringResource(
                        R.string.menu_configuracion,
                        uiState.negocioActivo?.nombre.orEmpty(),
                    ),
                    expandida = SeccionMenu.Configuracion in abiertas,
                    onToggle = { alternar(SeccionMenu.Configuracion) },
                ) {
                    configuracion.forEach { opcion ->
                        OpcionItem(opcion = opcion, onClick = { onOpcionClick(opcion) })
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        OpcionItem(
            opcion = OpcionMenu.CerrarSesion,
            onClick = { onOpcionClick(OpcionMenu.CerrarSesion) },
            contentColor = MaterialTheme.colorScheme.error,
            iconColor = MaterialTheme.colorScheme.error,
            colorResaltado = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@Preview(name = "Menú · Líder", showBackground = true, heightDp = 780)
@Preview(
    name = "Menú · Líder (oscuro)",
    showBackground = true,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MenuLateralPreview() {
    GestorVenturesTheme {
        MenuLateral(
            uiState = MenuLateralPreviewData.uiState,
            onNegocioClick = {},
            onAgregarNegocioClick = {},
            onOpcionClick = {},
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

/** Con el negocio del rol Vendedor activo no aparece la sección de configuración. */
@Preview(name = "Menú · Vendedor", showBackground = true, heightDp = 780)
@Composable
private fun MenuLateralVendedorPreview() {
    GestorVenturesTheme {
        MenuLateral(
            uiState = MenuLateralPreviewData.uiState.copy(negocioActivoId = "bella"),
            onNegocioClick = {},
            onAgregarNegocioClick = {},
            onOpcionClick = {},
            modifier = Modifier.fillMaxHeight(),
        )
    }
}
