package com.gestor_ventures.back.usecase

import com.gestor_ventures.back.model.ResultadoSugerencia
import com.gestor_ventures.back.repository.AsistenteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * HU-35. Le pide al asistente una idea de promoción para atraer más clientes.
 *
 * Usa los mismos datos del negocio que el chat ([ContextoDelNegocio]), con una tarea más
 * concreta: una sola promoción, realista, que no regale el margen y que diga por qué los datos
 * dicen que puede funcionar.
 */
class SugerirPromocion @Inject constructor(
    private val asistenteRepository: AsistenteRepository,
    private val contextoDelNegocio: ContextoDelNegocio,
) {

    suspend operator fun invoke(negocioId: Long): ResultadoSugerencia {
        // Los títulos de las últimas ideas van en el pedido para que no proponga lo mismo otra vez.
        val anteriores = asistenteRepository.sugerencias(negocioId).first().take(SugerenciasParaNoRepetir)
        val instrucciones = buildString {
            append(contextoDelNegocio.instruccionesPara(negocioId))
            appendLine()
            appendLine("## Tu tarea ahora")
            appendLine(
                "Propón UNA promoción concreta para atraer más clientes o hacer que vuelvan. Básala en " +
                    "los datos: los productos que más se venden, los días flojos y los clientes que " +
                    "dejaron de comprar. Tiene que ser realista para un negocio pequeño y no regalar " +
                    "el margen.",
            )
            appendLine(
                "Responde en este formato, sin nada más: una primera línea con el nombre de la " +
                    "promoción en negrita (**así**), y debajo, en máximo 70 palabras, qué es, a quién " +
                    "va dirigida, cuándo hacerla y por qué los datos dicen que puede funcionar.",
            )
            if (anteriores.isNotEmpty()) {
                appendLine("No repitas estas promociones que ya sugeriste:")
                anteriores.forEach { appendLine("- ${it.contenido.lineSequence().first()}") }
            }
        }
        return asistenteRepository.sugerirPromocion(negocioId, instrucciones, PedidoDelUsuario)
    }

    private companion object {
        const val SugerenciasParaNoRepetir = 5
        const val PedidoDelUsuario = "Sugiéreme una promoción para atraer más clientes."
    }
}
