package com.gestor_ventures.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * HU-01 a HU-04. Perfil del emprendedor.
 *
 * La autenticación (HU-01/02/03) la resuelve Firebase Authentication vía `AuthRepository`:
 * guarda y hashea la contraseña, valida credenciales y manda el enlace de recuperación. Esta
 * tabla solo guarda el perfil local que el resto de la app usa para relacionar negocios,
 * ventas, etc. con el usuario dueño, enlazado a Firebase por [correo] (único).
 *
 * [contrasenaHash], [tokenSesion], [codigoRecuperacion] y [fechaExpiracionCodigo] quedan sin
 * usar: se conservan por compatibilidad con instalaciones existentes en vez de forzar una
 * migración que reescriba la tabla completa.
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
