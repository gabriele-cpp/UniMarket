package com.example.unimarket.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
    @Test
    fun formatRupiah_usesDotAsThousandSeparator() {
        assertEquals("Rp85.000", formatRupiah(85_000))
        assertEquals("Rp1.250.000", formatRupiah(1_250_000))
        assertEquals("Rp500", formatRupiah(500))
        assertEquals("Rp0", formatRupiah(0))
    }

    @Test
    fun parsePrice_acceptsValidDigits() {
        assertEquals(85_000L, parsePrice("85000"))
        assertEquals(1L, parsePrice("1"))
    }

    @Test
    fun parsePrice_rejectsInvalidInput() {
        assertNull(parsePrice(""))
        assertNull(parsePrice("0"))
        assertNull(parsePrice("12a"))
        assertNull(parsePrice("-5"))
        assertNull(parsePrice("85.000"))
        assertNull(parsePrice("9999999999"))
    }
}
