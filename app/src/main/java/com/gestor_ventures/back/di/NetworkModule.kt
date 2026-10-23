package com.gestor_ventures.back.di

import com.gestor_ventures.back.network.GeminiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val UrlGemini = "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    fun proveerOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun proveerGeminiApi(cliente: OkHttpClient): GeminiApi = Retrofit.Builder()
        .baseUrl(UrlGemini)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)
}
