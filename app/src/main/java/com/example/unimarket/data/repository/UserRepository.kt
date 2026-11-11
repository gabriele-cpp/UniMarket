package com.example.unimarket.data.repository

import com.example.unimarket.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Sumber data pengguna, termasuk registrasi & login.
 * Implementasi utama: OfflineUserRepository (Room).
 */
interface UserRepository {
    /**
     * Mendaftarkan user baru. Password di-hash sebelum disimpan.
     * Error: [AuthErrors.EMAIL_TAKEN] jika email sudah dipakai.
     */
    suspend fun register(name: String, email: String, password: String, phone: String): AuthResult

    /** Error: [AuthErrors.INVALID_CREDENTIALS] jika email tidak ada atau password salah. */
    suspend fun login(email: String, password: String): AuthResult

    /** Mengubah nama dan nomor HP. Email & password tidak diubah di sini. */
    suspend fun updateProfile(userId: Long, name: String, phone: String)

    suspend fun getUserById(id: Long): UserEntity?

    suspend fun getUserByEmail(email: String): UserEntity?

    fun observeUser(id: Long): Flow<UserEntity?>
}
