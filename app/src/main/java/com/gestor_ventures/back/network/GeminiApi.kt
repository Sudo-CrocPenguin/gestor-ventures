package com.gestor_ventures.back.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * HU-34. La llamada a Gemini, el modelo de IA de Google que responde las preguntas.
 *
 * Es el único punto de la app que sale a internet. La clave va en un encabezado y no en la URL,
 * para que no quede escrita en los registros de las conexiones.
 */
interface GeminiApi {

    @POST("v1beta/models/{modelo}:generateContent")
    suspend fun generarContenido(
        @Path("modelo") modelo: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body peticion: PeticionGemini,
    ): RespuestaGemini
}
