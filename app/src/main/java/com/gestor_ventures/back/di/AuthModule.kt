package com.gestor_ventures.back.di

import com.gestor_ventures.back.repository.Autenticador
import com.gestor_ventures.back.repository.FirebaseAutenticador
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provee la instancia de Firebase Authentication que usa [FirebaseAutenticador]. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseAuthModule {

    @Provides
    @Singleton
    fun proveerFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}

/** Liga [Autenticador] a su única implementación real, para que las pruebas puedan reemplazarla. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutenticadorModule {

    @Binds
    abstract fun ligarAutenticador(impl: FirebaseAutenticador): Autenticador
}
