package com.example.unimarket.data.repository

import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.remote.SupabaseLoginRequest
import com.example.unimarket.data.remote.SupabaseSignUpRequest
import com.example.unimarket.data.remote.SupabaseUpdateProfileRequest
import com.example.unimarket.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

class SupabaseUserRepository(
    private val supabaseApi: SupabaseApi,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : UserRepository {

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String
    ): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()

        return try {
            val response = supabaseApi.signUp(
                SupabaseSignUpRequest(
                    email = trimmedEmail,
                    password = password,
                    data = mapOf(
                        "full_name" to trimmedName,
                        "phone" to phone
                    )
                )
            )

            val supabaseUser = response.user
                ?: return AuthResult.Error(AuthErrors.UNKNOWN)

            response.access_token?.let { userPreferences.setAccessToken(it) }
            supabaseUser.id?.let { userPreferences.setUserUuid(it) }

            // Sinkronkan ke Room agar relasi foreign key lokal tetap utuh
            val existingLocalUser = userDao.getUserByEmail(trimmedEmail)
            val localUser = if (existingLocalUser != null) {
                val updated = existingLocalUser.copy(
                    name = trimmedName,
                    phone = phone,
                    passwordHash = PasswordHasher.hash(password)
                )
                userDao.updateUser(updated)
                updated
            } else {
                val newUser = UserEntity(
                    name = trimmedName,
                    email = trimmedEmail,
                    passwordHash = PasswordHasher.hash(password),
                    phone = phone
                )
                val newId = userDao.upsertUser(newUser)
                newUser.copy(id = newId)
            }

            userPreferences.setLoggedInUserId(localUser.id)
            AuthResult.Success(localUser)

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            if (errorBody.contains("already registered", ignoreCase = true) ||
                errorBody.contains("already exists", ignoreCase = true) ||
                errorBody.contains("unique", ignoreCase = true)
            ) {
                AuthResult.Error(AuthErrors.EMAIL_TAKEN)
            } else {
                AuthResult.Error("Gagal mendaftar: ${e.message()}")
            }
        } catch (e: IOException) {
            AuthResult.Error("Tidak dapat terhubung ke server Supabase. Periksa koneksi internet Anda.")
        } catch (e: Exception) {
            AuthResult.Error(AuthErrors.UNKNOWN)
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim().lowercase()

        return try {
            val response = supabaseApi.login(
                SupabaseLoginRequest(
                    email = trimmedEmail,
                    password = password
                )
            )

            val supabaseUser = response.user
                ?: return AuthResult.Error(AuthErrors.INVALID_CREDENTIALS)

            response.access_token?.let { userPreferences.setAccessToken(it) }
            supabaseUser.id?.let { userPreferences.setUserUuid(it) }

            // Ambil detail profil dari Supabase atau metadata
            val profileName = supabaseUser.fullName ?: "Pengguna UniMarket"
            val profilePhone = supabaseUser.phone ?: ""

            // Sinkronkan atau buat data user lokal di Room
            val existing = userDao.getUserByEmail(trimmedEmail)
            val localUser = if (existing != null) {
                val updated = existing.copy(
                    name = profileName,
                    phone = if (profilePhone.isNotEmpty()) profilePhone else existing.phone,
                    passwordHash = if (existing.passwordHash.isNotEmpty()) existing.passwordHash else PasswordHasher.hash(password)
                )
                userDao.updateUser(updated)
                updated
            } else {
                val newUser = UserEntity(
                    name = profileName,
                    email = trimmedEmail,
                    passwordHash = PasswordHasher.hash(password),
                    phone = profilePhone
                )
                val newId = userDao.upsertUser(newUser)
                newUser.copy(id = newId)
            }

            userPreferences.setLoggedInUserId(localUser.id)
            AuthResult.Success(localUser)

        } catch (e: HttpException) {
            if (e.code() == 400) {
                AuthResult.Error(AuthErrors.INVALID_CREDENTIALS)
            } else {
                AuthResult.Error("Gagal masuk (${e.code()}): ${e.message()}")
            }
        } catch (e: IOException) {
            // Mode offline: jika koneksi internet mati, coba login lokal jika ada cache
            val localUser = userDao.getUserByEmail(trimmedEmail)
            if (localUser != null && PasswordHasher.verify(password, localUser.passwordHash)) {
                userPreferences.setLoggedInUserId(localUser.id)
                AuthResult.Success(localUser)
            } else {
                AuthResult.Error("Tidak dapat terhubung ke server dan tidak ada sesi offline yang valid.")
            }
        } catch (e: Exception) {
            AuthResult.Error(AuthErrors.UNKNOWN)
        }
    }

    override suspend fun updateProfile(userId: Long, name: String, phone: String) {
        val trimmedName = name.trim()
        val localUser = userDao.getUserById(userId) ?: return

        // 1. Update ke Supabase
        val uuid = userPreferences.getUserUuid()
        if (uuid != null) {
            try {
                supabaseApi.updateProfile(
                    idFilter = "eq.$uuid",
                    body = SupabaseUpdateProfileRequest(
                        full_name = trimmedName,
                        phone = phone
                    )
                )
            } catch (e: Exception) {
                // Abaikan kesalahan jaringan sementara, update lokal tetap berjalan
            }
        }

        // 2. Update di Room
        userDao.updateUser(localUser.copy(name = trimmedName, phone = phone))
    }

    override suspend fun getUserById(id: Long): UserEntity? =
        userDao.getUserById(id)

    override suspend fun getUserByEmail(email: String): UserEntity? =
        userDao.getUserByEmail(email.trim().lowercase())

    override fun observeUser(id: Long): Flow<UserEntity?> =
        userDao.observeUserById(id)
}
