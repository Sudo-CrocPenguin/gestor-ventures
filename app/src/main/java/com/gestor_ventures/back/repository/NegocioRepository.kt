package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.ErrorNegocio
import com.gestor_ventures.back.model.Negocio
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoNegocio
import com.gestor_ventures.back.model.TipoActividad
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.enums.TipoActividad as TipoActividadDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Tope de caracteres del nombre, como quedó especificado en HU-05. */
private const val MaxCaracteresNombre = 100

/**
 * HU-05 y HU-10. Única puerta de entrada a los datos de negocios.
 *
 * El `ViewModel` pide y recibe [Negocio]; acá adentro se traduce a la entidad de Room y se
 * validan las reglas.
 */
@Singleton
class NegocioRepository @Inject constructor(
    private val negocioDao: NegocioDao,
    private val reloj: Reloj,
) {

    /** Negocios del usuario, ordenados por nombre. Se actualiza solo al crear o editar uno. */
    fun negociosDeUsuario(usuarioId: Long): Flow<List<Negocio>> =
        negocioDao.observarDeUsuario(usuarioId).map { entidades -> entidades.map(::aNegocio) }

    fun observarNegocio(negocioId: Long): Flow<Negocio?> =
        negocioDao.observar(negocioId).map { entidad -> entidad?.let(::aNegocio) }

    /** Para saber si hay que mandar al usuario a crear su primer negocio. */
    suspend fun tieneNegocios(usuarioId: Long): Boolean =
        negocioDao.contarDeUsuario(usuarioId) > 0

    /** HU-05. Crea el negocio si los datos son válidos y devuelve su id. */
    suspend fun crearNegocio(
        usuarioId: Long,
        nombre: String,
        tipoActividad: TipoActividad,
        categoria: String,
        porcentajeReinversion: Double,
    ): ResultadoNegocio {
        val nombreLimpio = nombre.trim()
        val categoriaLimpia = categoria.trim()
        validar(nombreLimpio, categoriaLimpia, porcentajeReinversion)?.let {
            return ResultadoNegocio.Invalido(it)
        }

        val id = negocioDao.insertar(
            NegocioEntity(
                usuarioId = usuarioId,
                nombreNegocio = nombreLimpio,
                tipoActividad = tipoActividad.aDb(),
                categoriaNegocio = categoriaLimpia,
                porcentajeReinversion = porcentajeReinversion,
                fechaCreacion = reloj.ahora(),
            ),
        )
        return ResultadoNegocio.Exito(id)
    }

    /** HU-10. Edita un negocio existente conservando su fecha de creación. */
    suspend fun actualizarNegocio(
        negocioId: Long,
        nombre: String,
        tipoActividad: TipoActividad,
        categoria: String,
        porcentajeReinversion: Double,
    ): ResultadoNegocio {
        val nombreLimpio = nombre.trim()
        val categoriaLimpia = categoria.trim()
        validar(nombreLimpio, categoriaLimpia, porcentajeReinversion)?.let {
            return ResultadoNegocio.Invalido(it)
        }

        val actual = negocioDao.obtener(negocioId)
            ?: return ResultadoNegocio.Invalido(ErrorNegocio.NegocioNoExiste)

        negocioDao.actualizar(
            actual.copy(
                nombreNegocio = nombreLimpio,
                tipoActividad = tipoActividad.aDb(),
                categoriaNegocio = categoriaLimpia,
                porcentajeReinversion = porcentajeReinversion,
                fechaActualizacion = reloj.ahora(),
            ),
        )
        return ResultadoNegocio.Exito(negocioId)
    }

    private fun validar(
        nombre: String,
        categoria: String,
        porcentajeReinversion: Double,
    ): ErrorNegocio? = when {
        nombre.isEmpty() -> ErrorNegocio.NombreVacio
        nombre.length > MaxCaracteresNombre -> ErrorNegocio.NombreMuyLargo
        categoria.isEmpty() -> ErrorNegocio.CategoriaVacia
        porcentajeReinversion !in 0.0..100.0 -> ErrorNegocio.PorcentajeFueraDeRango
        else -> null
    }

    private fun aNegocio(entidad: NegocioEntity) = Negocio(
        id = entidad.negocioId,
        nombre = entidad.nombreNegocio,
        tipoActividad = entidad.tipoActividad.aDominio(),
        categoria = entidad.categoriaNegocio,
        porcentajeReinversion = entidad.porcentajeReinversion,
        fechaCreacion = entidad.fechaCreacion,
    )
}

private fun TipoActividad.aDb(): TipoActividadDb = when (this) {
    TipoActividad.SERVICIOS -> TipoActividadDb.SERVICIOS
    TipoActividad.PRODUCTOS -> TipoActividadDb.PRODUCTOS
    TipoActividad.MIXTO -> TipoActividadDb.MIXTO
}

private fun TipoActividadDb.aDominio(): TipoActividad = when (this) {
    TipoActividadDb.SERVICIOS -> TipoActividad.SERVICIOS
    TipoActividadDb.PRODUCTOS -> TipoActividad.PRODUCTOS
    TipoActividadDb.MIXTO -> TipoActividad.MIXTO
}
