package com.gestor_ventures.front.ui.menu

import com.gestor_ventures.front.model.NegocioUi
import com.gestor_ventures.front.model.RolNegocio
import com.gestor_ventures.front.model.UsuarioUi

/** Datos de ejemplo para los @Preview y para arrancar la UI mientras no existan los repositorios. */
object MenuLateralPreviewData {

    val usuario = UsuarioUi(nombre = "Sebastián Orrego", correo = "sebastian@correo.com")

    val negocios = listOf(
        NegocioUi(id = "dulce", nombre = "Dulce Antojo", categoria = "Repostería", rol = RolNegocio.Lider),
        NegocioUi(id = "bella", nombre = "Bella Piel", categoria = "Estética", rol = RolNegocio.Vendedor),
    )

    val uiState = MenuLateralUiState(
        usuario = usuario,
        negocios = negocios,
        negocioActivoId = "dulce",
    )
}
