package com.gestor_ventures.back.di

import android.content.Context
import androidx.room.Room
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.GestorVenturesDatabase
import com.gestor_ventures.db.MIGRACIONES
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.CategoriaDao
import com.gestor_ventures.db.dao.CostoDao
import com.gestor_ventures.db.dao.GastoDao
import com.gestor_ventures.db.dao.GastoFijoDao
import com.gestor_ventures.db.dao.MetaAhorroDao
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.ObligacionDao
import com.gestor_ventures.db.dao.UsuarioDao
import com.gestor_ventures.db.dao.VentaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDateTime
import javax.inject.Singleton

private const val NombreBaseDeDatos = "gestor_ventures.db"

/** Arma la base de datos y entrega los DAO a los repositorios. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun proveerBaseDeDatos(@ApplicationContext context: Context): GestorVenturesDatabase =
        Room.databaseBuilder(context, GestorVenturesDatabase::class.java, NombreBaseDeDatos)
            // TEMPORAL — se quita junto con SemillaTemporal cuando exista HU-01.
            .addCallback(SemillaTemporal.callback)
            .addMigrations(*MIGRACIONES)
            .build()

    @Provides
    fun proveerUsuarioDao(db: GestorVenturesDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun proveerNegocioDao(db: GestorVenturesDatabase): NegocioDao = db.negocioDao()

    @Provides
    fun proveerGastoFijoDao(db: GestorVenturesDatabase): GastoFijoDao = db.gastoFijoDao()

    @Provides
    fun proveerMetaAhorroDao(db: GestorVenturesDatabase): MetaAhorroDao = db.metaAhorroDao()

    @Provides
    fun proveerVentaDao(db: GestorVenturesDatabase): VentaDao = db.ventaDao()

    @Provides
    fun proveerCategoriaDao(db: GestorVenturesDatabase): CategoriaDao = db.categoriaDao()

    @Provides
    fun proveerGastoDao(db: GestorVenturesDatabase): GastoDao = db.gastoDao()

    @Provides
    fun proveerCostoDao(db: GestorVenturesDatabase): CostoDao = db.costoDao()

    @Provides
    fun proveerObligacionDao(db: GestorVenturesDatabase): ObligacionDao = db.obligacionDao()

    @Provides
    @Singleton
    fun proveerReloj(): Reloj = Reloj { LocalDateTime.now() }
}
