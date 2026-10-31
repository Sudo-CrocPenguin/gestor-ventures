package com.gestor_ventures.back.di

import com.gestor_ventures.back.network.GeminiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet6Address
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * HU-34. El cliente de internet de la app.
 *
 * La espera de respuesta es de un minuto, y no los 10 segundos por defecto: el modelo a veces
 * se toma su tiempo, y cortar antes sería tirar una respuesta que estaba por llegar.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val UrlGemini = "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    fun proveerOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .dns(PrimeroIPv4)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        // Tope para toda la llamada. Sin él, si la red no responde, la app podía quedarse en
        // "Revisando tus números…" minutos sin decir nada; con él, avisa que no hubo conexión.
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    /**
     * Prueba primero las direcciones IPv4 del servidor.
     *
     * Hay redes (la de varios módems y celulares) donde IPv6 anuncia ruta pero no entrega nada:
     * OkHttp prueba las direcciones una por una y cada intento sin respuesta gasta todo el tiempo
     * de conexión, así que con IPv6 primero la pregunta tardaba minutos en llegar a Gemini.
     */
    private val PrimeroIPv4 = Dns { nombre ->
        Dns.SYSTEM.lookup(nombre).sortedBy { it is Inet6Address }
    }

    @Provides
    @Singleton
    fun proveerGeminiApi(cliente: OkHttpClient): GeminiApi = Retrofit.Builder()
        .baseUrl(UrlGemini)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)
}
