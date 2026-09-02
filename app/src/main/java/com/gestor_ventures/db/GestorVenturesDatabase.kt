package com.gestor_ventures.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gestor_ventures.db.entity.AlertaEntity
import com.gestor_ventures.db.entity.CajaEntity
import com.gestor_ventures.db.entity.CategoriaEntity
import com.gestor_ventures.db.entity.CitaEntity
import com.gestor_ventures.db.entity.ClienteEntity
import com.gestor_ventures.db.entity.ConsultaIaEntity
import com.gestor_ventures.db.entity.CostoEntity
import com.gestor_ventures.db.entity.DetallePedidoEntity
import com.gestor_ventures.db.entity.GastoEntity
import com.gestor_ventures.db.entity.GastoFijoEntity
import com.gestor_ventures.db.entity.MetaAhorroEntity
import com.gestor_ventures.db.entity.NegocioEntity
import com.gestor_ventures.db.entity.NotificacionEntity
import com.gestor_ventures.db.entity.ObligacionEntity
import com.gestor_ventures.db.entity.PedidoEntity
import com.gestor_ventures.db.entity.RecordatorioClienteEntity
import com.gestor_ventures.db.entity.RecordatorioEntity
import com.gestor_ventures.db.entity.SugerenciaEntity
import com.gestor_ventures.db.entity.UsuarioEntity
import com.gestor_ventures.db.entity.VentaEntity

/**
 * Base de datos local (Room/SQLite) de Gestor Ventures. 20 entidades cubriendo las
 * Épicas 1 a 8 (HU-01 a HU-41). Los DAO se agregan en la siguiente etapa, ej.:
 *   abstract fun usuarioDao(): UsuarioDao
 */
@Database(
    entities = [
        UsuarioEntity::class,
        NegocioEntity::class,
        GastoFijoEntity::class,
        ObligacionEntity::class,
        MetaAhorroEntity::class,
        CategoriaEntity::class,
        VentaEntity::class,
        CostoEntity::class,
        GastoEntity::class,
        CajaEntity::class,
        CitaEntity::class,
        PedidoEntity::class,
        DetallePedidoEntity::class,
        RecordatorioEntity::class,
        ClienteEntity::class,
        RecordatorioClienteEntity::class,
        ConsultaIaEntity::class,
        SugerenciaEntity::class,
        AlertaEntity::class,
        NotificacionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GestorVenturesDatabase : RoomDatabase()
