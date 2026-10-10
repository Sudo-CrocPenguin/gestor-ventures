package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.Caja
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-20. Cuánta plata debería haber en la caja ahora mismo.
 *
 * No hay una tabla de movimientos de caja: las ventas, los gastos y los costos ya están
 * guardados, y la jornada es un periodo con una hora de inicio. Sumarlos por ese periodo evita
 * duplicar montos en dos lugares, que es como terminan dos cifras distintas para lo mismo.
 *
 * Los tres totales vienen por `Flow`, así que el saldo se recalcula solo al registrar cualquier
 * movimiento: nadie tiene que acordarse de avisarle a la caja.
 */
@Singleton
class CalcularSaldoEsperado @Inject constructor(
    private val cajaRepository: CajaRepository,
    private val ventaRepository: VentaRepository,
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
) {

    /** El saldo de la jornada en curso, o `null` si no hay ninguna caja abierta. */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(negocioId: Long): Flow<SaldoDeCaja?> =
        cajaRepository.cajaAbierta(negocioId).flatMapLatest { caja ->
            if (caja == null) {
                flowOf(null)
            } else {
                combine(
                    ventaRepository.totalDesde(negocioId, caja.fechaHoraApertura),
                    gastoRepository.totalDesde(negocioId, caja.fechaHoraApertura),
                    costoRepository.totalDesde(negocioId, caja.fechaHoraApertura),
                ) { ventas, gastos, costos ->
                    SaldoDeCaja(caja = caja, ventas = ventas, gastos = gastos, costos = costos)
                }
            }
        }
}

/**
 * HU-20. Lo que movió la jornada, desglosado.
 *
 * Se guardan las tres cifras por separado y no solo el total porque la pantalla las muestra: un
 * saldo que no cuadra no dice nada si no se puede ver de dónde salió.
 */
data class SaldoDeCaja(
    val caja: Caja,
    val ventas: Double,
    val gastos: Double,
    val costos: Double,
) {
    /** Todo lo que entró a la caja durante la jornada. */
    val entradas: Double get() = ventas

    /** Todo lo que salió: gastos generales (HU-14) y costos de producto (HU-13). */
    val salidas: Double get() = gastos + costos

    /** Con lo que se arrancó, más lo que entró, menos lo que salió. */
    val saldoEsperado: Double get() = caja.montoInicial + entradas - salidas
}
