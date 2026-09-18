package com.gestor_ventures.front.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * HU-16. Resumen del día del negocio activo.
 *
 * Las ventas (HU-11/HU-12) y la meta de ahorro (HU-08) son reales. Los gastos del día y las
 * cajas del equipo se muestran vacíos a propósito: preferimos que la pantalla diga "todavía no
 * hay" y no un número inventado que el usuario tome por bueno.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InicioViewModel @Inject constructor(
    negocioActivoRepository: NegocioActivoRepository,
    private val ventaRepository: VentaRepository,
    private val calcularResumenFinanciero: CalcularResumenFinanciero,
    private val reloj: Reloj,
) : ViewModel() {

    val uiState: StateFlow<InicioUiState> = negocioActivoRepository.negocioActivoId
        .flatMapLatest { negocioId ->
            if (negocioId == null) flowOf(estadoBase()) else resumenDe(negocioId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = estadoBase(),
        )

    private fun resumenDe(negocioId: Long): Flow<InicioUiState> {
        val hoy = reloj.ahora().toLocalDate()

        return combine(
            ventaRepository.ventasDelDia(negocioId, hoy),
            ventaRepository.resumenDelDia(negocioId, hoy),
            ventaRepository.resumenDelDia(negocioId, hoy.minusDays(1)),
            calcularResumenFinanciero.progresoDeLaMeta(negocioId),
        ) { ventasDeHoy, resumenHoy, resumenAyer, progresoMeta ->
            estadoBase(hoy).copy(
                resumenHoy = ResumenHoyUi(
                    ventas = resumenHoy.total,
                    variacionVentasVsAyer = variacion(resumenHoy.total, resumenAyer.total),
                    tendenciaVentas = tendenciaPorFranja(ventasDeHoy),
                    progresoMetaAhorro = progresoMeta?.fraccion,
                ),
                metaCumplida = progresoMeta?.cumplida == true,
            )
        }
    }

    /**
     * Cuánto subieron o bajaron las ventas frente a ayer, en porcentaje. Null si ayer no hubo
     * ventas: comparar contra cero no da un porcentaje, da un infinito.
     */
    private fun variacion(hoy: Double, ayer: Double): Int? =
        if (ayer <= 0.0) null else ((hoy - ayer) / ayer * 100).roundToInt()

    /**
     * Lo que se muestra mientras no hay negocio o no hay ventas.
     *
     * TEMPORAL — el nombre y las alertas salen de datos de ejemplo hasta que existan HU-01
     * (la cuenta del usuario) y HU-40 (notificaciones).
     */
    private fun estadoBase(hoy: LocalDate = reloj.ahora().toLocalDate()) = InicioUiState(
        nombreUsuario = InicioPreviewData.NombreTemporal,
        fecha = hoy,
        alertasNuevas = InicioPreviewData.AlertasTemporales,
    )
}
