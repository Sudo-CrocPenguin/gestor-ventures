package com.gestor_ventures.back.repository

import com.gestor_ventures.BuildConfig
import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MensajeAsistente
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoAsistente
import com.gestor_ventures.back.network.ContenidoGemini
import com.gestor_ventures.back.network.GeminiApi
import com.gestor_ventures.back.network.ParteGemini
import com.gestor_ventures.back.network.PeticionGemini
import com.gestor_ventures.db.dao.ConsultaIaDao
import com.gestor_ventures.db.entity.ConsultaIaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Rápido y con nivel gratuito: alcanza de sobra para resumir números. */
private const val ModeloGemini = "gemini-2.5-flash"

/**
 * HU-34. Habla con Gemini y guarda cada pregunta con su respuesta.
 *
 * Solo se guarda lo que salió bien: una pregunta sin respuesta no le sirve al historial, y la
 * pantalla se la devuelve al usuario para que la vuelva a enviar.
 */
@Singleton
class AsistenteRepository @Inject constructor(
    private val geminiApi: GeminiApi,
    private val consultaIaDao: ConsultaIaDao,
    private val reloj: Reloj,
) {

    fun mensajesDeSesion(negocioId: Long, sesionId: Long): Flow<List<MensajeAsistente>> =
        consultaIaDao.observarDeSesion(negocioId, sesionId).map { filas -> filas.map(::aMensaje) }

    /** Una conversación nueva se identifica con el momento en que empezó. */
    fun nuevaSesion(): Long = reloj.ahora().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * Manda la pregunta junto con lo ya hablado en la conversación, para que Gemini entienda
     * referencias como "¿y el mes anterior?". [instrucciones] trae las reglas y los datos del
     * negocio; se arman de nuevo en cada pregunta, así que siempre están al día.
     */
    suspend fun preguntar(
        negocioId: Long,
        sesionId: Long,
        pregunta: String,
        instrucciones: String,
    ): ResultadoAsistente {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return ResultadoAsistente.Fallo(ErrorAsistente.SinApiKey)

        val anteriores = consultaIaDao.deSesion(negocioId, sesionId).flatMap { consulta ->
            listOf(
                ContenidoGemini(role = "user", parts = listOf(ParteGemini(consulta.pregunta))),
                ContenidoGemini(role = "model", parts = listOf(ParteGemini(consulta.respuesta))),
            )
        }
        val peticion = PeticionGemini(
            systemInstruction = ContenidoGemini(parts = listOf(ParteGemini(instrucciones))),
            contents = anteriores + ContenidoGemini(role = "user", parts = listOf(ParteGemini(pregunta))),
        )

        val texto = try {
            geminiApi.generarContenido(ModeloGemini, apiKey, peticion)
                .candidates?.firstOrNull()?.content?.parts
                ?.joinToString("") { it.text }
                ?.trim()
        } catch (e: HttpException) {
            return ResultadoAsistente.Fallo(errorDe(e.code()))
        } catch (_: IOException) {
            return ResultadoAsistente.Fallo(ErrorAsistente.SinConexion)
        }
        if (texto.isNullOrEmpty()) return ResultadoAsistente.Fallo(ErrorAsistente.RespuestaVacia)

        val consulta = ConsultaIaEntity(
            negocioId = negocioId,
            sesionId = sesionId,
            pregunta = pregunta,
            respuesta = texto,
            fechaHora = reloj.ahora(),
        )
        val id = consultaIaDao.insertar(consulta)
        return ResultadoAsistente.Respuesta(aMensaje(consulta.copy(consultaId = id)))
    }

    /**
     * Qué quiere decir cada código de error de Gemini. Si la clave es inválida, responde 400
     * o 403; si se pasó el límite gratuito, 429.
     */
    private fun errorDe(codigo: Int): ErrorAsistente = when (codigo) {
        400, 401, 403 -> ErrorAsistente.ApiKeyInvalida
        429 -> ErrorAsistente.LimiteAlcanzado
        else -> ErrorAsistente.ServicioNoDisponible
    }

    private fun aMensaje(entidad: ConsultaIaEntity) = MensajeAsistente(
        id = entidad.consultaId,
        sesionId = entidad.sesionId,
        pregunta = entidad.pregunta,
        respuesta = entidad.respuesta,
        fechaHora = entidad.fechaHora,
    )
}
