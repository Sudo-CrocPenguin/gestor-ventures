package com.gestor_ventures.back.network

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

data class ConfiguracionGemini(
    val temperature: Double = 0.4,
    val thinkingConfig: ConfiguracionRazonamiento = ConfiguracionRazonamiento(),
)

data class ConfiguracionRazonamiento(val thinkingBudget: Int = 0)

data class RespuestaGemini(val candidates: List<CandidatoGemini>? = null)

data class CandidatoGemini(val content: ContenidoGemini? = null)
