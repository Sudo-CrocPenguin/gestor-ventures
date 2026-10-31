package com.gestor_ventures.back.di

import com.gestor_ventures.back.repository.AgendaDelDia
import com.gestor_ventures.back.repository.AgendaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgendaModule {

    @Binds
    @Singleton
    abstract fun enlazarAgendaDelDia(implementacion: AgendaRepository): AgendaDelDia
}
