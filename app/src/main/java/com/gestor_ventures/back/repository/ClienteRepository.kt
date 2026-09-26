package com.gestor_ventures.back.repository

import com.gestor_ventures.back.model.Cliente
import com.gestor_ventures.back.model.ErrorCliente
import com.gestor_ventures.back.model.FormatoCorreo
import com.gestor_ventures.back.model.LargoDeTelefono
import com.gestor_ventures.back.model.soloDigitos
import com.gestor_ventures.db.dao.ClienteDao
import com.gestor_ventures.db.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Tope de caracteres del nombre, el mismo que en el resto de la app. */
private const val MaxCaracteresNombre = 50

/**
 * HU-29. Los clientes del negocio.
 *
 * Las dos reglas que no son obvias:
 *
 * - El teléfono se guarda en puros dígitos aunque el usuario lo escriba con espacios o
 *   paréntesis. Si no, "300 111 2233" y "3001112233" serían dos clientes distintos.
 * - Teléfono y correo son opcionales, pero si están, no se pueden repetir dentro del mismo
 *   negocio: dos fichas de la misma persona parten su historial de compras en dos.
 */
@Singleton
class ClienteRepository @Inject constructor(
    private val clienteDao: ClienteDao,
) {

    fun clientesDeNegocio(negocioId: Long): Flow<List<Cliente>> =
        clienteDao.observarDeNegocio(negocioId).map { lista -> lista.map(::aCliente) }

    suspend fun obtenerCliente(clienteId: Long): Cliente? =
        clienteDao.obtener(clienteId)?.let(::aCliente)

    /** HU-29. Registra al cliente si los datos sirven y no chocan con otro del mismo negocio. */
    suspend fun crearCliente(
        negocioId: Long,
        nombre: String,
        telefono: String? = null,
        correo: String? = null,
        notas: String? = null,
    ): ErrorCliente? {
        val datos = limpiar(nombre, telefono, correo, notas)
        validar(negocioId, datos)?.let { return it }

        clienteDao.insertar(
            ClienteEntity(
                negocioId = negocioId,
                nombre = datos.nombre,
                telefono = datos.telefono,
                correo = datos.correo,
                notas = datos.notas,
            ),
        )
        return null
    }

    /**
     * HU-29. Corrige los datos de un cliente. Valida lo mismo que al crearlo.
     *
     * Si el cliente ya no existe no hace nada: se pudo borrar desde otra pantalla mientras el
     * formulario estaba abierto, y eso no es un error que el usuario deba resolver.
     */
    suspend fun editarCliente(
        clienteId: Long,
        nombre: String,
        telefono: String? = null,
        correo: String? = null,
        notas: String? = null,
    ): ErrorCliente? {
        val actual = clienteDao.obtener(clienteId) ?: return null
        val datos = limpiar(nombre, telefono, correo, notas)

        // Él mismo no cuenta como repetido: corregirle las notas no puede rebotar.
        validar(actual.negocioId, datos, exceptoId = clienteId)?.let { return it }

        clienteDao.actualizar(
            actual.copy(
                nombre = datos.nombre,
                telefono = datos.telefono,
                correo = datos.correo,
                notas = datos.notas,
            ),
        )
        return null
    }

    /**
     * HU-29. Borra al cliente.
     *
     * Sus ventas no se borran: la llave foránea está en SET_NULL, así que quedan sin cliente
     * pero siguen contando en las finanzas. Perder una venta por borrar una ficha sería mucho
     * peor que perder la ficha.
     */
    suspend fun eliminarCliente(clienteId: Long) {
        clienteDao.obtener(clienteId)?.let { clienteDao.eliminar(it) }
    }

    private fun limpiar(
        nombre: String,
        telefono: String?,
        correo: String?,
        notas: String?,
    ) = DatosDeCliente(
        nombre = nombre.trim(),
        // Un campo en blanco es "no me lo dio", no una cadena vacía guardada en la base.
        telefono = telefono?.let(::soloDigitos)?.takeIf { it.isNotEmpty() },
        correo = correo?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
        notas = notas?.trim()?.takeIf { it.isNotEmpty() },
    )

    private suspend fun validar(
        negocioId: Long,
        datos: DatosDeCliente,
        exceptoId: Long = 0,
    ): ErrorCliente? = when {
        datos.nombre.isEmpty() -> ErrorCliente.NombreVacio
        datos.nombre.length > MaxCaracteresNombre -> ErrorCliente.NombreMuyLargo
        datos.telefono != null && datos.telefono.length !in LargoDeTelefono ->
            ErrorCliente.TelefonoInvalido

        datos.correo != null && !FormatoCorreo.matches(datos.correo) -> ErrorCliente.CorreoInvalido

        datos.telefono != null &&
            clienteDao.existeConTelefono(negocioId, datos.telefono, exceptoId) > 0 ->
            ErrorCliente.TelefonoRepetido

        datos.correo != null &&
            clienteDao.existeConCorreo(negocioId, datos.correo, exceptoId) > 0 ->
            ErrorCliente.CorreoRepetido

        else -> null
    }

    private fun aCliente(entidad: ClienteEntity) = Cliente(
        id = entidad.clienteId,
        nombre = entidad.nombre,
        telefono = entidad.telefono,
        correo = entidad.correo,
        notas = entidad.notas,
    )
}

/** Los datos ya limpios, para no pasarlos sueltos entre limpiar y validar. */
private data class DatosDeCliente(
    val nombre: String,
    val telefono: String?,
    val correo: String?,
    val notas: String?,
)
