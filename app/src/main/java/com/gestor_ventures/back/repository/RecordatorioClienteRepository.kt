package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorRecordatorio
import com.gestor_ventures.back.model.RecordatorioCliente
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.TipoRecordatorio
import com.gestor_ventures.db.dao.RecordatorioClienteDao
import com.gestor_ventures.db.entity.RecordatorioClienteEntity
import com.gestor_ventures.db.enums.TipoRecordatorioCliente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-32. Los recordatorios que el negocio le manda a sus clientes.
 *
 * Esto no envía nada: el mensaje sale por WhatsApp, fuera de la app. Lo que hace es dejar
 * constancia de lo que se mandó, para que al abrir la ficha del cliente se sepa si ya se le
 * escribió y qué se le dijo.
 *
 * La fecha la pone el [Reloj], igual que en la caja: la que vale es cuándo se mandó de verdad.
 */
@Singleton
class RecordatorioClienteRepository @Inject constructor(
    private val recordatorioDao: RecordatorioClienteDao,
    private val reloj: Reloj,
) {

    fun recordatoriosDeCliente(clienteId: Long): Flow<List<RecordatorioCliente>> =
        recordatorioDao.observarDeCliente(clienteId).map { lista -> lista.map(::aRecordatorio) }

    /**
     * HU-32. Deja constancia de un recordatorio ya enviado.
     *
     * El mensaje se guarda recortado de espacios pero sin tocar nada más: es el texto que el
     * usuario decidió mandar, con sus correcciones, y no la plantilla de la que salió.
     */
    suspend fun registrarEnvio(
        clienteId: Long,
        tipo: TipoRecordatorio,
        mensaje: String,
    ): ErrorRecordatorio? {
        val limpio = mensaje.trim()
        if (limpio.isEmpty()) return ErrorRecordatorio.MensajeVacio

        recordatorioDao.insertar(
            RecordatorioClienteEntity(
                clienteId = clienteId,
                tipoRecordatorio = tipo.aDb(),
                mensaje = limpio,
                fechaEnvio = reloj.ahora(),
            ),
        )
        return null
    }

    private fun aRecordatorio(entidad: RecordatorioClienteEntity) = RecordatorioCliente(
        id = entidad.recordatorioClienteId,
        clienteId = entidad.clienteId,
        tipo = entidad.tipoRecordatorio.aDominio(),
        mensaje = entidad.mensaje,
        fechaEnvio = entidad.fechaEnvio,
    )
}

private fun TipoRecordatorio.aDb(): TipoRecordatorioCliente = when (this) {
    TipoRecordatorio.Cita -> TipoRecordatorioCliente.CITA
    TipoRecordatorio.PagoPendiente -> TipoRecordatorioCliente.PAGO_PENDIENTE
}

private fun TipoRecordatorioCliente.aDominio(): TipoRecordatorio = when (this) {
    TipoRecordatorioCliente.CITA -> TipoRecordatorio.Cita
    TipoRecordatorioCliente.PAGO_PENDIENTE -> TipoRecordatorio.PagoPendiente
}
