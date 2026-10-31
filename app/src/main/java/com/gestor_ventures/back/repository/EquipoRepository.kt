package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorEquipo
import com.gestor_ventures.back.model.EstadoMiembro
import com.gestor_ventures.back.model.FormatoCorreo
import com.gestor_ventures.back.model.MiembroEquipo
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.EquipoDao
import com.gestor_ventures.db.entity.MiembroEquipoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.gestor_ventures.db.enums.EstadoMiembro as EstadoMiembroDb

/**
 * El equipo de vendedores de cada negocio.
 *
 * Las reglas que importan: el correo es válido, no es el del propio Líder y no está ya en el
 * equipo (se compara sin distinguir mayúsculas: "Ana@x.com" y "ana@x.com" son la misma persona).
 */
@Singleton
class EquipoRepository @Inject constructor(
    private val equipoDao: EquipoDao,
    private val reloj: Reloj,
) {

    fun miembros(negocioId: Long): Flow<List<MiembroEquipo>> =
        equipoDao.observarDeNegocio(negocioId).map { filas -> filas.map(::aMiembro) }

    /** Invita a un vendedor. La invitación queda pendiente hasta que la acepte. */
    suspend fun invitar(negocioId: Long, correo: String, correoDelLider: String): ErrorEquipo? {
        val limpio = correo.trim().lowercase()
        return when {
            !FormatoCorreo.matches(limpio) -> ErrorEquipo.CorreoInvalido
            limpio == correoDelLider.trim().lowercase() -> ErrorEquipo.EsElLider
            equipoDao.contarPorCorreo(negocioId, limpio) > 0 -> ErrorEquipo.YaEstaEnElEquipo
            else -> {
                equipoDao.insertar(
                    MiembroEquipoEntity(
                        negocioId = negocioId,
                        correo = limpio,
                        estadoMiembro = EstadoMiembroDb.PENDIENTE,
                        fechaInvitacion = reloj.ahora(),
                    ),
                )
                null
            }
        }
    }

    /** Si alguien invitó a ese correo a algún negocio. */
    suspend fun tieneInvitacion(correo: String): Boolean =
        equipoDao.contarInvitaciones(correo.trim().lowercase()) > 0

    /** El vendedor entró con el correo de su invitación: pasa de pendiente a activo. */
    suspend fun aceptarInvitaciones(correo: String) =
        equipoDao.aceptarInvitaciones(correo.trim().lowercase())

    /** Quita al vendedor del equipo, o cancela su invitación si todavía no la aceptó. */
    suspend fun quitar(miembroId: Long) = equipoDao.eliminar(miembroId)

    private fun aMiembro(fila: MiembroEquipoEntity) = MiembroEquipo(
        id = fila.miembroEquipoId,
        correo = fila.correo,
        estado = when (fila.estadoMiembro) {
            EstadoMiembroDb.PENDIENTE -> EstadoMiembro.PENDIENTE
            EstadoMiembroDb.ACTIVO -> EstadoMiembro.ACTIVO
        },
        fechaInvitacion = fila.fechaInvitacion,
    )
}
