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

@Singleton
class RecordatorioRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val recordatorioDao: RecordatorioDao,
    private val citaDao: CitaDao,
    private val pedidoDao: PedidoDao,
    private val clienteDao: ClienteDao,
    private val reloj: Reloj,
) {

    suspend fun anticipacionDeCita(citaId: Long): AnticipacionRecordatorio? =
        AnticipacionRecordatorio.deMinutos(recordatorioDao.obtenerDeCita(citaId)?.tiempoAnticipacion)

    suspend fun anticipacionDePedido(pedidoId: Long): AnticipacionRecordatorio? =
        AnticipacionRecordatorio.deMinutos(recordatorioDao.obtenerDePedido(pedidoId)?.tiempoAnticipacion)

    suspend fun recordarCita(citaId: Long, anticipacion: AnticipacionRecordatorio?) {
        quitar(recordatorioDao.obtenerDeCita(citaId))
        val cita = citaDao.obtener(citaId) ?: return
        if (anticipacion == null || cita.estadoCita == EstadoCita.CANCELADA) return

        val id = recordatorioDao.insertar(
            RecordatorioEntity(citaId = citaId, tiempoAnticipacion = anticipacion.minutos),
        )
        programar(id, cita.fecha.atTime(cita.hora).minusMinutes(anticipacion.minutos.toLong()))
    }

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

    private fun programar(recordatorioId: Long, momento: LocalDateTime) {
        val espera = Duration.between(reloj.ahora(), momento).toMillis().coerceAtLeast(0)
        val tarea = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(espera, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(RecordatorioWorker.ClaveRecordatorio to recordatorioId))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(nombreDeTarea(recordatorioId), ExistingWorkPolicy.REPLACE, tarea)
    }

    private suspend fun quitar(recordatorio: RecordatorioEntity?) {
        recordatorio ?: return
        WorkManager.getInstance(context).cancelUniqueWork(nombreDeTarea(recordatorio.recordatorioId))
        recordatorioDao.eliminar(recordatorio)
    }

    private fun nombreDeTarea(recordatorioId: Long) = "recordatorio_$recordatorioId"
}
