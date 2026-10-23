package com.gestor_ventures.back.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gestor_ventures.back.model.AnticipacionRecordatorio
import com.gestor_ventures.back.model.ContenidoRecordatorio
import com.gestor_ventures.back.model.HoraDeReferenciaPedido
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.notificaciones.RecordatorioWorker
import com.gestor_ventures.db.dao.CitaDao
import com.gestor_ventures.db.dao.ClienteDao
import com.gestor_ventures.db.dao.PedidoDao
import com.gestor_ventures.db.dao.RecordatorioDao
import com.gestor_ventures.db.entity.RecordatorioEntity
import com.gestor_ventures.db.enums.EstadoCita
import com.gestor_ventures.db.enums.EstadoPedido
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-26. Guarda los recordatorios de citas y pedidos y programa su aviso.
 *
 * El aviso lo programa WorkManager, de Android, y no la app: así suena aunque la app esté
 * cerrada y aunque el teléfono se haya reiniciado. Cada recordatorio es una tarea con nombre
 * propio, para poder cancelarla o reemplazarla cuando la cita o el pedido cambian.
 */
@Singleton
class RecordatorioRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val recordatorioDao: RecordatorioDao,
    private val citaDao: CitaDao,
    private val pedidoDao: PedidoDao,
    private val clienteDao: ClienteDao,
    private val reloj: Reloj,
) {

    /** Cuánto antes avisa la cita, para mostrarlo al editarla. */
    suspend fun anticipacionDeCita(citaId: Long): AnticipacionRecordatorio? =
        AnticipacionRecordatorio.deMinutos(recordatorioDao.obtenerDeCita(citaId)?.tiempoAnticipacion)

    suspend fun anticipacionDePedido(pedidoId: Long): AnticipacionRecordatorio? =
        AnticipacionRecordatorio.deMinutos(recordatorioDao.obtenerDePedido(pedidoId)?.tiempoAnticipacion)

    /**
     * Deja la cita con el recordatorio indicado, o sin ninguno si es null. Siempre quita el
     * anterior primero: así, al mover la cita, el aviso se mueve con ella. Una cita cancelada
     * no lleva recordatorio.
     */
    suspend fun recordarCita(citaId: Long, anticipacion: AnticipacionRecordatorio?) {
        quitar(recordatorioDao.obtenerDeCita(citaId))
        val cita = citaDao.obtener(citaId) ?: return
        if (anticipacion == null || cita.estadoCita == EstadoCita.CANCELADA) return

        val id = recordatorioDao.insertar(
            RecordatorioEntity(citaId = citaId, tiempoAnticipacion = anticipacion.minutos),
        )
        programar(id, cita.fecha.atTime(cita.hora).minusMinutes(anticipacion.minutos.toLong()))
    }

    /** Igual que [recordarCita], contando desde las 8:00 del día de entrega. */
    suspend fun recordarPedido(pedidoId: Long, anticipacion: AnticipacionRecordatorio?) {
        quitar(recordatorioDao.obtenerDePedido(pedidoId))
        val pedido = pedidoDao.obtener(pedidoId) ?: return
        if (anticipacion == null || pedido.estadoPedido == EstadoPedido.CANCELADO) return

        val id = recordatorioDao.insertar(
            RecordatorioEntity(pedidoId = pedidoId, tiempoAnticipacion = anticipacion.minutos),
        )
        val entrega = pedido.fechaEntrega.atTime(HoraDeReferenciaPedido)
        programar(id, entrega.minusMinutes(anticipacion.minutos.toLong()))
    }

    suspend fun quitarDeCita(citaId: Long) {
        quitar(recordatorioDao.obtenerDeCita(citaId))
    }

    suspend fun quitarDePedido(pedidoId: Long) {
        quitar(recordatorioDao.obtenerDePedido(pedidoId))
    }

    /**
     * Lo que dice el aviso, o null si ya no hay que avisar: ya se mandó, la cita no está
     * programada, o el pedido se canceló o ya se entregó. Se revisa justo antes de sonar,
     * porque entre que se programó y que suena pudo pasar cualquier cosa.
     */
    suspend fun contenidoDe(recordatorioId: Long): ContenidoRecordatorio? {
        val recordatorio = recordatorioDao.obtener(recordatorioId) ?: return null
        if (recordatorio.enviado) return null

        recordatorio.citaId?.let { citaId ->
            val cita = citaDao.obtener(citaId) ?: return null
            if (cita.estadoCita != EstadoCita.PROGRAMADA) return null
            val cliente = clienteDao.obtener(cita.clienteId) ?: return null
            return ContenidoRecordatorio.DeCita(cliente.nombre, cita.fecha, cita.hora)
        }
        recordatorio.pedidoId?.let { pedidoId ->
            val pedido = pedidoDao.obtener(pedidoId) ?: return null
            if (pedido.estadoPedido == EstadoPedido.CANCELADO || pedido.estadoPedido == EstadoPedido.ENTREGADO) {
                return null
            }
            val cliente = clienteDao.obtener(pedido.clienteId) ?: return null
            return ContenidoRecordatorio.DePedido(cliente.nombre, pedido.fechaEntrega)
        }
        return null
    }

    suspend fun marcarEnviado(recordatorioId: Long) {
        recordatorioDao.marcarEnviado(recordatorioId)
    }

    /**
     * Programa el aviso para [momento]. Si ese momento ya pasó —"1 día antes" de una cita que
     * es en dos horas— suena enseguida: avisar tarde es mejor que no avisar.
     */
    private fun programar(recordatorioId: Long, momento: LocalDateTime) {
        val espera = Duration.between(reloj.ahora(), momento).toMillis().coerceAtLeast(0)
        val tarea = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(espera, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(RecordatorioWorker.ClaveRecordatorio to recordatorioId))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(nombreDeTarea(recordatorioId), ExistingWorkPolicy.REPLACE, tarea)
    }

    /** Cancela el aviso programado y borra el recordatorio. */
    private suspend fun quitar(recordatorio: RecordatorioEntity?) {
        recordatorio ?: return
        WorkManager.getInstance(context).cancelUniqueWork(nombreDeTarea(recordatorio.recordatorioId))
        recordatorioDao.eliminar(recordatorio)
    }

    private fun nombreDeTarea(recordatorioId: Long) = "recordatorio_$recordatorioId"
}
