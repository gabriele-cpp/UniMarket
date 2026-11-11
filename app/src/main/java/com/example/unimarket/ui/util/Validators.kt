package com.example.unimarket.ui.util

import android.util.Patterns

/**
 * Validasi input form. Setiap fungsi mengembalikan pesan error (bahasa Indonesia) atau null jika valid.
 */
object Validators {
    const val MIN_PASSWORD_LENGTH = 6

    fun name(value: String): String? =
        if (value.isBlank()) "Nama wajib diisi" else null

    fun email(value: String): String? = when {
        value.isBlank() -> "Email wajib diisi"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Format email tidak valid"
        else -> null
    }

    fun phone(value: String): String? {
        val digits = value.trim()
        return when {
            digits.isEmpty() -> "Nomor HP wajib diisi"
            !digits.all { it.isDigit() } -> "Nomor HP hanya boleh berisi angka"
            !(digits.startsWith("08") || digits.startsWith("62")) -> "Nomor HP harus diawali 08 atau 62"
            digits.length !in 10..14 -> "Nomor HP harus 10–14 digit"
            else -> null
        }
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password wajib diisi"
        value.length < MIN_PASSWORD_LENGTH -> "Password minimal $MIN_PASSWORD_LENGTH karakter"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Konfirmasi password wajib diisi"
        confirm != password -> "Konfirmasi password tidak sama"
        else -> null
    }
}

/** Inisial nama, contoh "Budi Santoso" -> "BS". */
fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "UM"
    }
}
