package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * HU-01 a HU-04. Perfil del emprendedor.
 *
 * Nota de seguridad: [contrasenaHash], [tokenSesion] y [codigoRecuperacion] cubren
 * HU-01/02/03 tal como quedaron especificadas en los issues. En producción es preferible
 * delegar la autenticación a un backend y guardar solo el token localmente en
 * almacenamiento cifrado (DataStore/EncryptedSharedPreferences), no en Room.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long = 0,

    val nombre: String, // máx. 50 caracteres — validar en capa de aplicación

    val correo: String, // único, formato usuario@dominio.extensión

    @ColumnInfo(name = "contrasena_hash")
    val contrasenaHash: String, // hash Argon2

    @ColumnInfo(name = "foto_perfil_url")
    val fotoPerfilUrl: String? = null,

    val telefono: String? = null, // solo dígitos

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: LocalDateTime,

    @ColumnInfo(name = "fecha_ultimo_acceso")
    val fechaUltimoAcceso: LocalDateTime? = null,

    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: LocalDateTime? = null,

    @ColumnInfo(name = "token_sesion")
    val tokenSesion: String? = null,

    @ColumnInfo(name = "codigo_recuperacion")
    val codigoRecuperacion: String? = null,

    @ColumnInfo(name = "fecha_expiracion_codigo")
    val fechaExpiracionCodigo: LocalDateTime? = null
)
