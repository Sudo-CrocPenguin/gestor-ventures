package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.CajaCerrada
import com.gestor_ventures.back.model.DiferenciaDeCaja
import com.gestor_ventures.back.model.RangoFechas
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.saldoDeCaja
import com.gestor_ventures.back.repository.CajaRepository
import com.gestor_ventures.back.repository.CostoRepository
import com.gestor_ventures.back.repository.GastoRepository
import com.gestor_ventures.back.repository.VentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-23. El historial de jornadas cerradas, cada una con su diferencia.
 *
 * La diferencia no está guardada en la tabla: se vuelve a calcular con los movimientos que
 * cayeron dentro de cada jornada. Eso cuesta tres consultas por caja, pero tiene una ventaja
 * que vale el precio: si alguien corrige una venta mal registrada (HU-18), el historial se
 * corrige solo. Un número copiado al cerrar se habría quedado mintiendo para siempre.
 */
@Singleton
class ListarCierresDeCaja @Inject constructor(
    private val cajaRepository: CajaRepository,
    private val ventaRepository: VentaRepository,
    private val gastoRepository: GastoRepository,
    private val costoRepository: CostoRepository,
    private val reloj: Reloj,
) {

    operator fun invoke(negocioId: Long, rango: RangoFechas): Flow<List<CierreDeCaja>> =
        cajaRepository.cajasCerradasEntre(negocioId, rango.desde, rango.hasta)
            .map { cajas -> cajas.map { caja -> CierreDeCaja(caja, esperadoDe(negocioId, caja)) } }

    /** El día de hoy, para resolver los periodos de un toque contra el reloj de la app. */
    fun hoy(): LocalDate = reloj.ahora().toLocalDate()

    /**
     * Lo que debería haber habido en esa caja al cerrarla.
     *
     * El periodo de una jornada cerrada tiene principio y final de verdad, así que acá sí se
     * pregunta una sola vez en vez de quedarse escuchando como hace la caja en curso.
     */
    private suspend fun esperadoDe(negocioId: Long, caja: CajaCerrada): Double = saldoDeCaja(
        montoInicial = caja.montoInicial,
        ventas = ventaRepository.sumarEntre(negocioId, caja.apertura, caja.cierre),
        gastos = gastoRepository.sumarEntre(negocioId, caja.apertura, caja.cierre),
        costos = costoRepository.sumarEntre(negocioId, caja.apertura, caja.cierre),
    )
}

/** HU-23. Una jornada del historial: cómo terminó y si cuadró. */
data class CierreDeCaja(
    val caja: CajaCerrada,
    val esperado: Double,
) {
    val diferencia: DiferenciaDeCaja
        get() = DiferenciaDeCaja(esperado = esperado, real = caja.montoReal)
}

/** HU-23. Lo que resume el periodo completo, para la cabecera del historial. */
data class ResumenDeCierres(
    val cierres: Int,
    val conDiferencia: Int,
)

/** Cuántas jornadas hubo y en cuántas no cuadró la caja. */
fun List<CierreDeCaja>.resumen(): ResumenDeCierres = ResumenDeCierres(
    cierres = size,
    conDiferencia = count { !it.diferencia.cuadra },
)
