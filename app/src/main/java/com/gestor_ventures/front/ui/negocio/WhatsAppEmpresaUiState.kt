package com.gestor_ventures.front.ui.negocio

/**
 * El WhatsApp con el que el negocio le escribe a sus clientes.
 *
 * [vinculado] es el celular ya guardado (solo dígitos); [numero] es lo que hay escrito en el
 * campo, que puede ser otro mientras el usuario lo cambia.
 */
data class WhatsAppEmpresaUiState(
    val nombreNegocio: String = "",
    val vinculado: String? = null,
    val numero: String = "",
    val numeroInvalido: Boolean = false,
    val sinNegocio: Boolean = false,
) {
    val puedeGuardar: Boolean get() = !sinNegocio && numero.isNotBlank()
}
