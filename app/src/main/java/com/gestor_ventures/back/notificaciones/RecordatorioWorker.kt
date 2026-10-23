package com.gestor_ventures.back.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gestor_ventures.R
import com.gestor_ventures.back.model.ContenidoRecordatorio
import com.gestor_ventures.back.repository.RecordatorioRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * HU-26. La tarea que WorkManager corre a la hora del recordatorio: arma el texto y muestra la
 * notificación.
 *
 * WorkManager crea esta clase por su cuenta, así que Hilt no le puede pasar el repositorio por
 * el constructor. Lo pide con [Dependencias], que es la forma de Hilt de entregar algo a una
 * clase que no construye él.
 */
class RecordatorioWorker(
    context: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(context, parametros) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun recordatorioRepository(): RecordatorioRepository
    }

    override suspend fun doWork(): Result {
        val recordatorioId = inputData.getLong(ClaveRecordatorio, -1L)
        if (recordatorioId < 0) return Result.failure()

        val repositorio = EntryPointAccessors
            .fromApplication(applicationContext, Dependencias::class.java)
            .recordatorioRepository()
        val contenido = repositorio.contenidoDe(recordatorioId) ?: return Result.success()

        val (titulo, texto) = when (contenido) {
            is ContenidoRecordatorio.DeCita -> applicationContext.getString(R.string.notificacion_cita_titulo) to
                applicationContext.getString(
                    R.string.notificacion_cita_texto,
                    contenido.nombreCliente,
                    formatoDia(contenido.fecha),
                    contenido.hora.format(DateTimeFormatter.ofPattern("H:mm")),
                )
            is ContenidoRecordatorio.DePedido -> applicationContext.getString(R.string.notificacion_pedido_titulo) to
                applicationContext.getString(
                    R.string.notificacion_pedido_texto,
                    contenido.nombreCliente,
                    formatoDia(contenido.fechaEntrega),
                )
        }

        Notificador.mostrar(applicationContext, recordatorioId.toInt(), Notificador.CanalAgenda, titulo, texto)
        repositorio.marcarEnviado(recordatorioId)
        return Result.success()
    }

    /** "viernes 24 de octubre". */
    private fun formatoDia(fecha: LocalDate): String =
        fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CO")))

    companion object {
        const val ClaveRecordatorio = "recordatorio_id"
    }
}
