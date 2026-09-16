package com.gestor_ventures.back.di

import android.content.Context
import androidx.room.Room
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.db.GestorVenturesDatabase
import com.gestor_ventures.db.SemillaTemporal
import com.gestor_ventures.db.dao.NegocioDao
import com.gestor_ventures.db.dao.UsuarioDao
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
            .build()

    @Provides
    fun proveerUsuarioDao(db: GestorVenturesDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun proveerNegocioDao(db: GestorVenturesDatabase): NegocioDao = db.negocioDao()

    @Provides
    @Singleton
    fun proveerReloj(): Reloj = Reloj { LocalDateTime.now() }
}
