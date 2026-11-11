package com.example.unimarket.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneUtilsTest {
    @Test
    fun normalizePhone_convertsLocalFormat() {
        assertEquals("6281234567890", normalizePhone("081234567890"))
    }

    @Test
    fun normalizePhone_keepsInternationalFormat() {
        assertEquals("6281234567890", normalizePhone("6281234567890"))
    }

    @Test
    fun normalizePhone_stripsSymbolsAndSpaces() {
        assertEquals("6281234567890", normalizePhone("+62 812-3456-7890"))
        assertEquals("6281234567890", normalizePhone(" 0812 3456 7890 "))
    }

    @Test
    fun phoneForDisplay_convertsBackToLocal() {
        assertEquals("081234567890", phoneForDisplay("6281234567890"))
    }

    @Test
    fun buildWhatsAppUrl_encodesMessage() {
        val url = buildWhatsAppUrl("081234567890", whatsAppGreeting("Kalkulator FX-991 & Cover"))
        assertEquals(
            "https://wa.me/6281234567890?text=" +
                "Halo%2C%20saya%20tertarik%20dengan%20Kalkulator%20FX-991%20%26%20Cover%20di%20UniMarket.",
            url
        )
    }
}
