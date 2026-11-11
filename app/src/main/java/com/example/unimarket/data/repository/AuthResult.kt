package com.example.unimarket.data.repository

import com.example.unimarket.data.local.UserEntity

/**
 * Hasil register/login. Pesan error sudah dalam bahasa Indonesia, siap ditampilkan di UI.
 */
sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object AuthErrors {
    const val EMAIL_TAKEN = "Email sudah terdaftar"
    const val INVALID_CREDENTIALS = "Email atau password salah"
    const val UNKNOWN = "Terjadi kesalahan, silakan coba lagi"
}
