package com.example.unimarket.ui.util

import com.example.unimarket.data.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Catatan: Validators.email() memakai android.util.Patterns sehingga tidak diuji di unit test JVM.
class ValidatorsTest {
    @Test
    fun phone_acceptsValidNumbers() {
        assertNull(Validators.phone("0812345678"))       // 10 digit
        assertNull(Validators.phone("081234567890"))
        assertNull(Validators.phone("6281234567890"))
        assertNull(Validators.phone("62812345678901"))   // 14 digit
    }

    @Test
    fun phone_rejectsInvalidNumbers() {
        assertNotNull(Validators.phone(""))
        assertNotNull(Validators.phone("081234567"))         // 9 digit
        assertNotNull(Validators.phone("081234567890123"))   // 15 digit
        assertNotNull(Validators.phone("0912345678"))        // bukan 08/62
        assertNotNull(Validators.phone("08123-45678"))
    }

    @Test
    fun password_rules() {
        assertNotNull(Validators.password("12345"))
        assertNull(Validators.password("123456"))
        assertNotNull(Validators.confirmPassword("123456", "1234567"))
        assertNull(Validators.confirmPassword("123456", "123456"))
    }

    @Test
    fun name_required() {
        assertNotNull(Validators.name("   "))
        assertNull(Validators.name("Kenny"))
    }

    @Test
    fun normalizePhone_convertsLocalToInternational() {
        assertEquals("6281234567890", normalizePhone("081234567890"))
        assertEquals("6281234567890", normalizePhone("6281234567890"))
        assertEquals("081234567890", phoneForDisplay("6281234567890"))
    }

    @Test
    fun initials_fromName() {
        assertEquals("GE", initials("Gabriel Emil"))
        assertEquals("KE", initials("kenny"))
        assertEquals("UM", initials("  "))
    }

    @Test
    fun passwordHasher_hashesWithSaltAndVerifies() {
        val h1 = PasswordHasher.hash("rahasia123")
        val h2 = PasswordHasher.hash("rahasia123")
        assertNotEquals("Salt acak -> hash berbeda", h1, h2)
        assertFalse(h1.contains("rahasia123"))
        assertTrue(PasswordHasher.verify("rahasia123", h1))
        assertFalse(PasswordHasher.verify("salah", h1))
        assertFalse(PasswordHasher.verify("rahasia123", "format-rusak"))
    }
}
