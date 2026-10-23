package com.gestor_ventures.back.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApi {

    @POST("v1beta/models/{modelo}:generateContent")
    suspend fun generarContenido(
        @Path("modelo") modelo: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body peticion: PeticionGemini,
    ): RespuestaGemini
}
