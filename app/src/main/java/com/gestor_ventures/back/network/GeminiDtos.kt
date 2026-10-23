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
 */
data class ConfiguracionGemini(
    val temperature: Double = 0.4,
    val thinkingConfig: ConfiguracionRazonamiento = ConfiguracionRazonamiento(),
)

/**
 * Gemini 2.5 puede "pensar" antes de responder. Para resumir cifras que ya vienen calculadas no
 * hace falta, y apagarlo hace que responda mucho más rápido.
 */
data class ConfiguracionRazonamiento(val thinkingBudget: Int = 0)

data class RespuestaGemini(val candidates: List<CandidatoGemini>? = null)

data class CandidatoGemini(val content: ContenidoGemini? = null)
