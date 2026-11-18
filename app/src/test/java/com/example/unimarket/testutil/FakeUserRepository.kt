package com.example.unimarket.testutil

import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.repository.AuthErrors
import com.example.unimarket.data.repository.AuthResult
import com.example.unimarket.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake untuk [UserRepository] (asli: OfflineUserRepository + Room + PasswordHasher).
 * Password disimpan apa adanya di memori: hashing bukan yang diuji di test ViewModel.
 * Email dinormalisasi (trim + lowercase) seperti implementasi asli.
 */
class FakeUserRepository(initial: List<Pair<UserEntity, String>> = emptyList()) : UserRepository {

    /** user → password. */
    private val users = MutableStateFlow(initial)

    /** Jika diisi, semua fungsi suspend melempar exception (DB rusak). */
    var error: Exception? = null

    /** > 0 untuk menguji state loading saat login/register. */
    var delayMillis: Long = 0

    val current: List<UserEntity> get() = users.value.map { it.first }

    override suspend fun register(name: String, email: String, password: String, phone: String): AuthResult {
        simulate()
        val normalized = email.trim().lowercase()
        if (users.value.any { it.first.email == normalized }) return AuthResult.Error(AuthErrors.EMAIL_TAKEN)
        val newUser = UserEntity(
            id = (users.value.maxOfOrNull { it.first.id } ?: 0L) + 1,
            name = name.trim(),
            email = normalized,
            passwordHash = "",
            phone = phone,
            createdAt = 0L,
        )
        users.update { it + (newUser to password) }
        return AuthResult.Success(newUser)
    }

    override suspend fun login(email: String, password: String): AuthResult {
        simulate()
        val match = users.value.firstOrNull { it.first.email == email.trim().lowercase() }
        return if (match != null && match.second == password) {
            AuthResult.Success(match.first)
        } else {
            AuthResult.Error(AuthErrors.INVALID_CREDENTIALS)
        }
    }

    override suspend fun updateProfile(userId: Long, name: String, phone: String) {
        simulate()
        users.update { list ->
            list.map { (u, pw) -> if (u.id == userId) u.copy(name = name.trim(), phone = phone) to pw else u to pw }
        }
    }

    override suspend fun getUserById(id: Long): UserEntity? {
        simulate()
        return current.firstOrNull { it.id == id }
    }

    override suspend fun getUserByEmail(email: String): UserEntity? {
        simulate()
        return current.firstOrNull { it.email == email.trim().lowercase() }
    }

    override fun observeUser(id: Long): Flow<UserEntity?> =
        users.map { list -> list.firstOrNull { it.first.id == id }?.first }

    private suspend fun simulate() {
        if (delayMillis > 0) delay(delayMillis)
        error?.let { throw it }
    }
}
