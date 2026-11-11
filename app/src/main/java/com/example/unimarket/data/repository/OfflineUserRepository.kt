package com.example.unimarket.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow

class OfflineUserRepository(private val userDao: UserDao) : UserRepository {

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String
    ): AuthResult {
        val normalizedEmail = normalizeEmail(email)
        if (userDao.getUserByEmail(normalizedEmail) != null) {
            return AuthResult.Error(AuthErrors.EMAIL_TAKEN)
        }
        return try {
            val user = UserEntity(
                name = name.trim(),
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash(password),
                phone = phone
            )
            val id = userDao.insertUser(user)
            AuthResult.Success(user.copy(id = id))
        } catch (e: SQLiteConstraintException) {
            // Jaga-jaga jika email didaftarkan bersamaan (unique index).
            AuthResult.Error(AuthErrors.EMAIL_TAKEN)
        } catch (e: Exception) {
            AuthResult.Error(AuthErrors.UNKNOWN)
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val user = userDao.getUserByEmail(normalizeEmail(email))
            ?: return AuthResult.Error(AuthErrors.INVALID_CREDENTIALS)
        return if (PasswordHasher.verify(password, user.passwordHash)) {
            AuthResult.Success(user)
        } else {
            AuthResult.Error(AuthErrors.INVALID_CREDENTIALS)
        }
    }

    override suspend fun updateProfile(userId: Long, name: String, phone: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(name = name.trim(), phone = phone))
    }

    override suspend fun getUserById(id: Long): UserEntity? =
        userDao.getUserById(id)

    override suspend fun getUserByEmail(email: String): UserEntity? =
        userDao.getUserByEmail(normalizeEmail(email))

    override fun observeUser(id: Long): Flow<UserEntity?> =
        userDao.observeUserById(id)

    private fun normalizeEmail(email: String): String = email.trim().lowercase()
}
