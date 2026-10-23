package com.gestor_ventures.back.repository

import com.gestor_ventures.BuildConfig
import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MensajeAsistente
import com.gestor_ventures.back.model.Reloj
import com.gestor_ventures.back.model.ResultadoAsistente
import com.gestor_ventures.back.model.ResultadoSugerencia
import com.gestor_ventures.back.model.Sugerencia
import com.gestor_ventures.back.network.ContenidoGemini
import com.gestor_ventures.back.network.GeminiApi
import com.gestor_ventures.back.network.ParteGemini
import com.gestor_ventures.back.network.PeticionGemini
import com.gestor_ventures.db.dao.ConsultaIaDao
import com.gestor_ventures.db.dao.SugerenciaDao
import com.gestor_ventures.db.entity.ConsultaIaEntity
import com.gestor_ventures.db.entity.SugerenciaEntity
import com.gestor_ventures.db.enums.EstadoSugerencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Rápido y con nivel gratuito: alcanza de sobra para resumir números. */
/** La tabla de sugerencias admite otros tipos más adelante; por ahora solo hay promociones. */
private const val TipoPromocion = "promocion"

private const val ModeloGemini = "gemini-2.5-flash"

/**
 * HU-34 y HU-35. Habla con Gemini y guarda cada pregunta con su respuesta, y cada idea de
 * promoción.
 *
 * Solo se guarda lo que salió bien: una pregunta sin respuesta no le sirve al historial, y la
 * pantalla se la devuelve al usuario para que la vuelva a enviar.
 */
@Singleton
class AsistenteRepository @Inject constructor(
    private val geminiApi: GeminiApi,
    private val consultaIaDao: ConsultaIaDao,
    private val sugerenciaDao: SugerenciaDao,
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
        val anteriores = consultaIaDao.deSesion(negocioId, sesionId).flatMap { consulta ->
            listOf(
                ContenidoGemini(role = "user", parts = listOf(ParteGemini(consulta.pregunta))),
                ContenidoGemini(role = "model", parts = listOf(ParteGemini(consulta.respuesta))),
            )
        }
        val texto = when (val generado = generar(instrucciones, anteriores + mensajeDelUsuario(pregunta))) {
            is Generado.Texto -> generado.texto
            is Generado.Error -> return ResultadoAsistente.Fallo(generado.error)
        }

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
    /** HU-35. Las ideas nuevas y las guardadas. Se actualiza sola al guardar o descartar. */
    fun sugerencias(negocioId: Long): Flow<List<Sugerencia>> =
        sugerenciaDao.observarVisibles(negocioId).map { filas -> filas.map(::aSugerencia) }

    /** HU-35. Le pide a Gemini una idea y la guarda como nueva, sin decidir. */
    suspend fun sugerirPromocion(negocioId: Long, instrucciones: String, pedido: String): ResultadoSugerencia {
        val texto = when (val generado = generar(instrucciones, listOf(mensajeDelUsuario(pedido)))) {
            is Generado.Texto -> generado.texto
            is Generado.Error -> return ResultadoSugerencia.Fallo(generado.error)
        }
        val sugerencia = SugerenciaEntity(
            negocioId = negocioId,
            tipoSugerencia = TipoPromocion,
            contenido = texto,
            fechaGeneracion = reloj.ahora(),
        )
        val id = sugerenciaDao.insertar(sugerencia)
        return ResultadoSugerencia.Generada(aSugerencia(sugerencia.copy(sugerenciaId = id)))
    }

    suspend fun guardarSugerencia(sugerenciaId: Long) {
        sugerenciaDao.actualizarEstado(sugerenciaId, EstadoSugerencia.GUARDADA)
    }

    suspend fun descartarSugerencia(sugerenciaId: Long) {
        sugerenciaDao.actualizarEstado(sugerenciaId, EstadoSugerencia.DESCARTADA)
    }

    /**
     * La llamada a Gemini, compartida por el chat y las promociones: revisa la clave, manda el
     * pedido y traduce los errores. Así los dos fallan igual y con los mismos avisos.
     */
    private suspend fun generar(instrucciones: String, contenidos: List<ContenidoGemini>): Generado {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return Generado.Error(ErrorAsistente.SinApiKey)

        val peticion = PeticionGemini(
            systemInstruction = ContenidoGemini(parts = listOf(ParteGemini(instrucciones))),
            contents = contenidos,
        )
        val texto = try {
            geminiApi.generarContenido(ModeloGemini, apiKey, peticion)
                .candidates?.firstOrNull()?.content?.parts
                ?.joinToString("") { it.text }
                ?.trim()
        } catch (e: HttpException) {
            return Generado.Error(errorDe(e.code()))
        } catch (_: IOException) {
            return Generado.Error(ErrorAsistente.SinConexion)
        }
        return if (texto.isNullOrEmpty()) Generado.Error(ErrorAsistente.RespuestaVacia) else Generado.Texto(texto)
    }

    private fun mensajeDelUsuario(texto: String) =
        ContenidoGemini(role = "user", parts = listOf(ParteGemini(texto)))

    /** Lo que devuelve [generar]: el texto, o qué salió mal. */
    private sealed interface Generado {
        data class Texto(val texto: String) : Generado
        data class Error(val error: ErrorAsistente) : Generado
    }

    private fun aSugerencia(entidad: SugerenciaEntity) = Sugerencia(
        id = entidad.sugerenciaId,
        contenido = entidad.contenido,
        guardada = entidad.estadoSugerencia == EstadoSugerencia.GUARDADA,
        fechaGeneracion = entidad.fechaGeneracion,
    )

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
