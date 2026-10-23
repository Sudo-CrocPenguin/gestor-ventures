package com.gestor_ventures.back.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.gestor_ventures.back.model.CambioDeFoto
import com.gestor_ventures.back.model.ErrorPerfil
import com.gestor_ventures.back.model.FormatosDeFotoPerfil
import com.gestor_ventures.back.model.LargoDeTelefono
import com.gestor_ventures.back.model.MaxBytesFotoPerfil
import com.gestor_ventures.back.model.MaxCaracteresNombreUsuario
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.dao.UsuarioDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU-04. Guarda los cambios del perfil del emprendedor: nombre, teléfono y foto.
 *
 * El correo no se edita acá: es con el que la cuenta entra a Firebase, y cambiarlo sería otra
 * historia.
 *
 * La foto no se guarda en la base de datos. Se copia a una carpeta privada de la app y en la
 * tabla `usuarios` queda solo la dirección de esa copia. Así la foto sigue viéndose aunque el
 * usuario borre la original de su galería.
 */
@Singleton
class PerfilRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val usuarioDao: UsuarioDao,
    private val reloj: Reloj,
) {

    /**
     * Valida y guarda el perfil, y anota la fecha de actualización. Devuelve null si todo salió
     * bien, o el error si algo no cumple las reglas; en ese caso no se guarda nada.
     *
     * La foto vieja se borra solo después de guardar, para no quedarse sin ninguna si algo
     * falla en el medio.
     */
    suspend fun actualizarPerfil(
        usuarioId: Long,
        nombre: String,
        telefono: String,
        foto: CambioDeFoto,
    ): ErrorPerfil? = withContext(Dispatchers.IO) {
        val nombreLimpio = nombre.trim()
        val telefonoLimpio = telefono.trim().takeIf { it.isNotEmpty() }

        validar(nombreLimpio, telefonoLimpio)?.let { return@withContext it }
        if (foto is CambioDeFoto.Nueva) validarFoto(foto.uri.toUri())?.let { return@withContext it }

        val actual = usuarioDao.obtener(usuarioId) ?: return@withContext ErrorPerfil.UsuarioNoExiste

        val fotoGuardada = when (foto) {
            CambioDeFoto.SinCambios -> actual.fotoPerfilUrl
            CambioDeFoto.Quitar -> null
            is CambioDeFoto.Nueva -> copiarFoto(usuarioId, foto.uri.toUri())
                ?: return@withContext ErrorPerfil.FotoIlegible
        }

        usuarioDao.actualizar(
            actual.copy(
                nombre = nombreLimpio,
                telefono = telefonoLimpio,
                fotoPerfilUrl = fotoGuardada,
                fechaActualizacion = reloj.ahora(),
            ),
        )

        if (fotoGuardada != actual.fotoPerfilUrl) borrarFoto(actual.fotoPerfilUrl)
        null
    }

    /**
     * El nombre no puede quedar vacío ni pasar de 50 caracteres. El teléfono es opcional, pero
     * si se escribe tiene que ser solo números y tener entre 7 y 15 dígitos.
     */
    private fun validar(nombre: String, telefono: String?): ErrorPerfil? = when {
        nombre.isEmpty() -> ErrorPerfil.NombreVacio
        nombre.length > MaxCaracteresNombreUsuario -> ErrorPerfil.NombreMuyLargo
        telefono != null && telefono.any { !it.isDigit() } -> ErrorPerfil.TelefonoInvalido
        telefono != null && telefono.length !in LargoDeTelefono -> ErrorPerfil.TelefonoInvalido
        else -> null
    }

    /** La foto tiene que ser JPG, PNG o WEBP y pesar máximo 5 MB. */
    private fun validarFoto(uri: Uri): ErrorPerfil? {
        val tipo = context.contentResolver.getType(uri)
        if (tipo !in FormatosDeFotoPerfil) return ErrorPerfil.FotoFormatoNoSoportado

        val tamano = tamanoDe(uri) ?: return ErrorPerfil.FotoIlegible
        return if (tamano > MaxBytesFotoPerfil) ErrorPerfil.FotoMuyPesada else null
    }

    /**
     * Cuánto pesa la foto. Primero se le pregunta a Android; si no lo sabe, se lee el archivo y
     * se cuenta, parando apenas pasa del máximo para no leer de más una foto enorme.
     */
    private fun tamanoDe(uri: Uri): Long? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getLong(0)
            }
        return try {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                var total = 0L
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val leidos = entrada.read(buffer)
                    if (leidos < 0 || total > MaxBytesFotoPerfil) break
                    total += leidos
                }
                total
            }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Copia la foto elegida a la carpeta privada de la app y devuelve su dirección. El nombre
     * lleva la hora para que cada foto nueva sea un archivo distinto y la pantalla no muestre
     * una vieja guardada en memoria.
     */
    private fun copiarFoto(usuarioId: Long, uri: Uri): String? {
        val extension = FormatosDeFotoPerfil[context.contentResolver.getType(uri)] ?: return null
        val carpeta = File(context.filesDir, CarpetaFotos).apply { mkdirs() }
        val destino = File(carpeta, "usuario_${usuarioId}_${System.currentTimeMillis()}.$extension")

        return try {
            val entrada = context.contentResolver.openInputStream(uri) ?: return null
            entrada.use { origen -> destino.outputStream().use { origen.copyTo(it) } }
            destino.toUri().toString()
        } catch (_: IOException) {
            destino.delete()
            null
        }
    }

    /** Borra una foto vieja, pero solo si es una copia de la app: nunca toca la galería. */
    private fun borrarFoto(fotoUrl: String?) {
        val archivo = fotoUrl?.toUri()?.path?.let(::File) ?: return
        val carpeta = File(context.filesDir, CarpetaFotos)
        if (archivo.parentFile == carpeta) archivo.delete()
    }

    private companion object {
        const val CarpetaFotos = "fotos_perfil"
    }
}
