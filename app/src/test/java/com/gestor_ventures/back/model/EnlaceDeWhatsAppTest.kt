package com.gestor_ventures.back.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * HU-31. El enlace de WhatsApp a partir del teléfono guardado.
 *
 * Lo delicado es el indicativo: WhatsApp necesita el número en formato internacional y la app
 * lo guarda como lo escribió el usuario, casi siempre sin él.
 */
class EnlaceDeWhatsAppTest {

    @Test
    fun unCelularColombianoRecibeElIndicativo() {
        assertEquals("https://wa.me/573001112233", enlaceDeWhatsApp("3001112233"))
    }

    @Test
    fun unNumeroQueYaTraeIndicativoNoLoRecibeDosVeces() {
        // Doce dígitos: ya viene en formato internacional.
        assertEquals("https://wa.me/573001112233", enlaceDeWhatsApp("573001112233"))
    }

    @Test
    fun elNumeroSeLimpiaAntesDeArmarElEnlace() {
        // Aunque se guarde en dígitos (HU-29), lo que llegue acá puede traer formato.
        assertEquals("https://wa.me/573001112233", enlaceDeWhatsApp("(300) 111-2233"))
    }

    @Test
    fun sinTelefonoNoHayEnlace() {
        assertNull(enlaceDeWhatsApp(null))
    }

    @Test
    fun unTelefonoVacioTampocoDaEnlace() {
        assertNull(enlaceDeWhatsApp(""))
    }

    @Test
    fun unTelefonoDePuroFormatoSinDigitosTampoco() {
        // "()- " no es un número; sin dígitos no hay a quién escribirle.
        assertNull(enlaceDeWhatsApp("()- "))
    }

    @Test
    fun unFijoDeSieteDigitosTambienRecibeElIndicativo() {
        assertEquals("https://wa.me/571234567", enlaceDeWhatsApp("1234567"))
    }
}
