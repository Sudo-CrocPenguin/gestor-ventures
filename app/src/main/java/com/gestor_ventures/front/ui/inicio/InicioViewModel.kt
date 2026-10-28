package com.gestor_ventures.front.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.Venta
import com.gestor_ventures.back.repository.AlertaRepository
import com.gestor_ventures.back.repository.NegocioActivoRepository
import com.gestor_ventures.back.repository.SesionRepository
import com.gestor_ventures.back.repository.VentaRepository
import com.gestor_ventures.back.usecase.CalcularResumenFinanciero
import com.gestor_ventures.back.usecase.CalcularSaldoEsperado
import com.gestor_ventures.back.usecase.GenerarAlertas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/** Cuántas ventas del día caben en el inicio antes de que deje de ser un resumen. */
private const val MaxUltimasVentas = 5

/**
 * HU-16. Resumen del día del negocio activo.
 *
 * Las ventas (HU-11/HU-12), la meta de ahorro (HU-08), la caja del turno (HU-19/HU-20), las
 * alertas (HU-38) y el nombre de quien entró (HU-01) son reales. Los gastos del día se
 * muestran vacíos a propósito: preferimos que la pantalla diga "todavía no hay" y no un número
 * que el usuario tome por bueno.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val negocioActivoRepository: NegocioActivoRepository,
    private val sesionRepository: SesionRepository,
    private val ventaRepository: VentaRepository,
    private val alertaRepository: AlertaRepository,
    private val calcularResumenFinanciero: CalcularResumenFinanciero,
    private val calcularSaldoEsperado: CalcularSaldoEsperado,
    private val generarAlertas: GenerarAlertas,
    private val reloj: Reloj,
) : ViewModel() {

    init {
        // HU-38. Se revisan al abrir el inicio y no solo al entrar a notificaciones: el inicio
        // es la primera pantalla, y un aviso que solo aparece si vas a buscarlo no avisa nada.
        // Repetirlo no duplica: `GenerarAlertas` descarta lo que ya avisó en el periodo.
        viewModelScope.launch {
            negocioActivoRepository.negocioActivoId.first()?.let { generarAlertas(it) }
        }
    }

    val uiState: StateFlow<InicioUiState> = negocioActivoRepository.negocioActivoId
        .flatMapLatest { negocioId ->
            // Sin negocio todavía hay a quién saludar: la cuenta existe antes que el negocio.
            if (negocioId == null) {
                sesionRepository.usuarioActual.map { estadoBase(nombre = it?.primerNombre.orEmpty()) }
            } else {
                resumenDe(negocioId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = estadoBase(),
        )

    private fun resumenDe(negocioId: Long): Flow<InicioUiState> =
        // La caja y las alertas van aparte y no dentro del `combine` de abajo porque ese ya
        // llegó a cinco flujos, que es el tope de esa forma de combinar.
        combine(
            resumenDelDia(negocioId),
            calcularSaldoEsperado(negocioId),
            alertaRepository.alertas(negocioId),
        ) { estado, saldo, alertas ->
            estado.copy(
                cajaAbierta = saldo?.let {
                    CajaAbiertaUi(
                        horaApertura = it.caja.fechaHoraApertura.toLocalTime(),
                        montoInicial = it.caja.montoInicial,
                        saldoEsperado = it.saldoEsperado,
                    )
                },
                alertas = alertas,
            )
        }

    private fun resumenDelDia(negocioId: Long): Flow<InicioUiState> {
        val hoy = reloj.ahora().toLocalDate()

        return combine(
            ventaRepository.ventasDelDia(negocioId, hoy),
            ventaRepository.resumenDelDia(negocioId, hoy),
            ventaRepository.resumenDelDia(negocioId, hoy.minusDays(1)),
            calcularResumenFinanciero.progresoDeLaMeta(negocioId),
            sesionRepository.usuarioActual,
        ) { ventasDeHoy, resumenHoy, resumenAyer, progresoMeta, usuario ->
            estadoBase(hoy, usuario?.primerNombre.orEmpty()).copy(
                resumenHoy = ResumenHoyUi(
                    ventas = resumenHoy.total,
                    variacionVentasVsAyer = variacion(resumenHoy.total, resumenAyer.total),
                    tendenciaVentas = tendenciaPorFranja(ventasDeHoy),
                    progresoMetaAhorro = progresoMeta?.fraccion,
                ),
                metaCumplida = progresoMeta?.cumplida == true,
                ultimasVentas = ultimasVentas(ventasDeHoy),
            )
        }
    }

    /**
     * Las últimas ventas del día, de la más reciente a la más vieja.
     *
     * Se recortan a [MaxUltimasVentas] porque el inicio es un resumen: un día bueno trae
     * decenas de ventas y la pantalla dejaría de servir para echar un vistazo. Para verlas
     * todas está el historial (HU-15).
     */
    private fun ultimasVentas(ventas: List<Venta>): List<VentaDeHoyUi> =
        ventas.sortedByDescending { it.fechaHora }
            .take(MaxUltimasVentas)
            .map { venta ->
                VentaDeHoyUi(
                    id = venta.id,
                    titulo = venta.productoServicio.orEmpty(),
                    monto = venta.monto,
                    hora = venta.fechaHora.toLocalTime(),
                )
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
     * El nombre llega vacío hasta que la sesión se resuelve, y entonces la pantalla saluda sin
     * nombre: mejor eso que un "Hola, " colgando o un nombre inventado.
     */
    private fun estadoBase(
        hoy: LocalDate = reloj.ahora().toLocalDate(),
        nombre: String = "",
    ) = InicioUiState(
        nombreUsuario = nombre,
        fecha = hoy,
    )
}
