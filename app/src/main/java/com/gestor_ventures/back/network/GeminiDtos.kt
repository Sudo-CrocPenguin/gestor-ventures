package com.gestor_ventures.back.network

/**
 * HU-34. Lo que se le manda a Gemini y lo que devuelve, con los nombres de campo que pide su
 * API (por eso están en inglés).
 *
 * [systemInstruction] lleva las reglas y los datos del negocio; [contents], la conversación.
 */
data class PeticionGemini(
    val systemInstruction: ContenidoGemini,
    val contents: List<ContenidoGemini>,
    val generationConfig: ConfiguracionGemini = ConfiguracionGemini(),
)

data class ContenidoGemini(
    val parts: List<ParteGemini>,
    val role: String? = null,
)

data class ParteGemini(val text: String)

/**
 * Temperatura baja para que no se ponga creativo con las cifras: al hablar de plata, la
 * respuesta tiene que ser la misma cada vez.
 *
 * No se configura el "razonamiento" del modelo: antes se mandaba `thinkingBudget = 0` para que
 * respondiera más rápido, pero los modelos nuevos lo rechazan con un 400 y el asistente dejaba
 * de funcionar. El modelo "lite" que se usa ya responde en un par de segundos sin tocar eso.
 */
data class ConfiguracionGemini(
    val temperature: Double = 0.4,
)

data class RespuestaGemini(val candidates: List<CandidatoGemini>? = null)

data class CandidatoGemini(val content: ContenidoGemini? = null)
