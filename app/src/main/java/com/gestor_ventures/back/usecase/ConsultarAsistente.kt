package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.ErrorAsistente
import com.gestor_ventures.back.model.MaxCaracteresPregunta
import com.gestor_ventures.back.model.ResultadoAsistente
import com.gestor_ventures.back.repository.AsistenteRepository
import javax.inject.Inject

/**
 * HU-34, HU-36 y HU-37. Le pregunta al asistente por las ventas del negocio, por cuánto puede
 * reinvertir y por en qué está gastando de más, con sus números reales.
 *
 * Valida la pregunta y se la pasa a [AsistenteRepository] junto con el contexto que arma
 * [ContextoDelNegocio].
 */
class ConsultarAsistente @Inject constructor(
    private val asistenteRepository: AsistenteRepository,
    private val contextoDelNegocio: ContextoDelNegocio,
) {

    suspend operator fun invoke(negocioId: Long, sesionId: Long, pregunta: String): ResultadoAsistente {
        val limpia = pregunta.trim()
        if (limpia.isEmpty()) return ResultadoAsistente.Fallo(ErrorAsistente.PreguntaVacia)
        if (limpia.length > MaxCaracteresPregunta) return ResultadoAsistente.Fallo(ErrorAsistente.PreguntaMuyLarga)

        return asistenteRepository.preguntar(
            negocioId = negocioId,
            sesionId = sesionId,
            pregunta = limpia,
            instrucciones = contextoDelNegocio.instruccionesPara(negocioId),
        )
    }
}
