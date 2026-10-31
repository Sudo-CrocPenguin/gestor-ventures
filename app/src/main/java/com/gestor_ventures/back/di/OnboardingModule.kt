package com.gestor_ventures.back.di

import com.gestor_ventures.back.repository.Onboarding
import com.gestor_ventures.back.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    @Singleton
    abstract fun enlazarOnboarding(implementacion: OnboardingRepository): Onboarding
}
