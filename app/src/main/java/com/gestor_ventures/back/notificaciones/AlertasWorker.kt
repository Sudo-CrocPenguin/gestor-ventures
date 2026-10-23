package com.gestor_ventures.back.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gestor_ventures.R
import com.gestor_ventures.back.model.TipoAlerta
import com.gestor_ventures.back.usecase.GenerarAlertas
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * HU-38. La revisión diaria del negocio, aunque la app esté cerrada. Cada alerta nueva llega
 * como notificación por el canal de alertas.
 *
 * Igual que [RecordatorioWorker], pide sus dependencias a Hilt con un `EntryPoint` porque
 * WorkManager construye la clase por su cuenta.
 */
class AlertasWorker(
    context: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(context, parametros) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun generarAlertas(): GenerarAlertas
    }

    override suspend fun doWork(): Result {
        val generarAlertas = EntryPointAccessors
            .fromApplication(applicationContext, Dependencias::class.java)
            .generarAlertas()

        generarAlertas.paraTodosLosNegocios().forEach { alerta ->
            Notificador.mostrar(
                context = applicationContext,
                id = alerta.hashCode(),
                canal = Notificador.CanalAlertas,
                titulo = applicationContext.getString(tituloDe(alerta.tipo)),
                texto = alerta.descripcion,
            )
        }
        return Result.success()
    }

    private fun tituloDe(tipo: TipoAlerta): Int = when (tipo) {
        TipoAlerta.CaidaVentas -> R.string.alerta_caida_ventas
        TipoAlerta.GastoElevado -> R.string.alerta_gasto_elevado
        TipoAlerta.MetaEnRiesgo -> R.string.alerta_meta_en_riesgo
    }

    companion object {
        private const val NombreTarea = "alertas_diarias"

        /**
         * Deja programada la revisión una vez al día. Con KEEP, abrir la app de nuevo no la
         * reinicia: sigue el horario que ya tenía.
         */
        fun programar(context: Context) {
            val tarea = PeriodicWorkRequestBuilder<AlertasWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NombreTarea, ExistingPeriodicWorkPolicy.KEEP, tarea)
        }
    }
}
